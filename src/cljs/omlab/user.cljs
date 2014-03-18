(ns omlab.user
  (:require-macros [cljs.core.async.macros :refer [go alt!]])
  (:require [clojure.string :as string]
            [cljs.core.async :refer [put! <! >! chan timeout] :as async]
            [secretary.core :as secretary]
            [om.core :as om :include-macros true]
            [om.dom :as dom :include-macros true]
            [cljs-http.client :as http]
            [omlab.http :as xhr :include-macros true :refer [go-fetch]]
            [omlab.util :refer [guid now] :as util]
            [omlab.format :refer [as-str] :as format]
            [omlab.dom :as ldom]
            [omlab.handler :refer [update-value! handle-app-event] :as handler]
            [omlab.notification :refer [show-notification hide-notification] :as notification]))

(defn fetch-users [app url]
  (go-fetch
   [{status :status users :body} (<! (http/get url {:timeout xhr/default-timeout}))]
   (->> users
        (map #(assoc % :email-hash (util/email-digest (:email %))))
        (map util/update-ids)
        (reduce #(assoc %1 (:username %2) %2) {}))
   (show-notification app (str "Fetching omlab users failed. Status code: " status) :type :error)))

(defn fetch-user-detail [app url]
  (go-fetch
   [{status :status user :body} (<! (http/get url {:timeout xhr/default-timeout}))]
   (util/with-guid (assoc user :email-hash (util/email-digest (:email user))))
   (show-notification app (str "Fetching user detail failed. Status code: " status) :type :error)))

(defn load-profile [app]
  (go (let [detail (<! (fetch-user-detail app "/ui/profile"))
            profile (assoc detail :old-password "")]
        (om/update! app [:user-profile] profile))))

(defmethod handle-app-event :edit-user edit-user [type app username]
  (om/transact! app [:users username]
                (fn [user] (assoc user :unedited user :editing true))))

(defmethod handle-app-event :cancel-user-editing cancel-user-editing [type app username]
  (om/transact! app [:users]
               (fn [users] (assoc users username (get-in users [username :unedited])))))

(defmethod handle-app-event :save-user save-user [type app username]
  (let [user (get-in @app [:users username])
        params (select-keys user [:username :name :email :type :roles :password :optlock])
        notifo-guid (show-notification app (str "Saving user " (:username user)))]
    (go (let [res (<! (http/post (str "/admin/user/" username) {:edn-params params :timeout xhr/default-timeout}))]
          (hide-notification app notifo-guid)
          (if (<= 200 (:status res) 299)
            (do
              (handle-app-event :cancel-user-editing app username)
              (show-notification app (str "User " username " has been successfully saved!") :type :success :ttl (/ notification/default-ttl 3)))
            (show-notification app (str "User " username "can't be saved! " (-> res :body :msg) " Error code: " (:status res)) :type :error :ttl (* 2 notification/default-ttl)))))))

(defmethod handle-app-event :save-user-profile save-user-profile [type app user]
  (let [profile (select-keys user [:name :email :old-password :password :optlock])
        params (if (= "" (-> profile :password str string/trim))
                 (dissoc profile :password)
                 profile)
        notifo-guid (show-notification app (str "Saving profile of " (:username profile)))]
    (go (let [{:keys [status] :as res} (<! (http/post "/ui/profile" {:edn-params params :timeout xhr/default-timeout}))]
          (hide-notification app notifo-guid)
          (cond
           (<= 200 status 299)
           (do
             (load-profile app)
             (show-notification app "Your profile has been successfully saved! Please sign-in again." :type :success :ttl (/ notification/default-ttl 3)))

           (= status 403)
           (do
             (om/update! app [:user-profile :old-password-invalid] true)
             (show-notification app "Your Current Password is invalid!" :type :error))

           :else
           (show-notification app (str "Your profile can't be saved! " (-> res :body :msg) " Error code: " (:status res)) :type :error :ttl (* 2 notification/default-ttl)))))))

(defmethod handle-app-event :add-user add-user [type app user]
  (when-let [username (:username user)]
    (let [params (select-keys user [:username :name :email :type :roles :password])
          notifo-guid (show-notification app (str "Adding user " username))]
      (go (let [{:keys [status body] :as res} (<! (http/put (str "/admin/user/" username) {:edn-params params :timeout xhr/default-timeout}))]
            (hide-notification app notifo-guid)
            (if (<= 200 status 299)
              (do
                (om/transact! app [:users] (fn [users] (assoc users username user)))
                (secretary/dispatch! "/admin/users")
                (show-notification app (str "User " username " has been successfully added!") :type :success :ttl (/ notification/default-ttl 3)))
              (show-notification app (str "User can't be added! " (:msg body) " Error code: " status) :type :error :ttl (* 2 notification/default-ttl))))))))

(defn- passwords-match? [state]
  (= (get-in state [:password :first-val])
     (get-in state [:password :retype-val])))

(defn handle-password [e cursor key owner state]
  (let [val (.. e -target -value)]
    (om/set-state! owner [:password key] val)
    (if (passwords-match? (om/get-state owner))
      (om/transact! cursor #(assoc % :password val :password-valid true))
      (om/update! cursor :password-valid false))))

(defn password-fields [cursor owner opts]
  (reify
    om/IWillMount
    (will-mount [_]
      (om/set-state! owner :password {:first-val "" :retype-val ""}))

    om/IRenderState
    (render-state [_ {:keys [comm] :as state}]
      (dom/ul #js {:className (str "omlab-field"
                                   (when (false? (:password-valid cursor)) " has-error"))}
              (dom/li nil
                      (dom/label #js {:className "omlab-label"}
                                 (dom/span #js {:className "omlab-label"} "Password")
                                 (dom/input #js {:className "form-control input-medium"
                                                 :type "password" :value (get-in state [:password :first-val])
                                                 :onChange #(handle-password % cursor :first-val owner state)})))
              (dom/li nil
                      (dom/label #js {:className "omlab-label"}
                                 (dom/span #js {:className "omlab-label"} "Retype\u00A0Password")
                                 (dom/input #js {:className "form-control input-medium"
                                                 :type "password" :value (get-in state [:password :retype-val])
                                                 :onChange #(handle-password % cursor :retype-val owner state)})))))))

(defn users-header [app]
  (dom/div #js {:className "row user-header"}
           (dom/div #js {:className "col-md-2"} (dom/b nil "Username"))
           (dom/div #js {:className "col-md-3"} (dom/b nil "Full Name"))
           (dom/div #js {:className "col-md-3"} (dom/b nil "E-mail"))
           (dom/div #js {:className "col-md-2"} (dom/b nil "Roles"))
           (dom/div #js {:className "col-md-1"} (dom/b nil "Type"))
           (dom/div #js {:className "col-md-1"} (dom/b nil "Avatar"))))

(defn user-short [{:keys [username] :as user} owner _]
  (reify
    om/IRenderState
    (render-state [_ {:keys [comm]}]
      (dom/div #js {:className "row"}
               (dom/div #js {:className "col-md-2"}
                        (dom/a #js {:href (str "#/admin/users/show/" username)} username))
               (dom/div #js {:className "col-md-3"} (:name user))
               (dom/div #js {:className "col-md-3"} (:email user))
               (dom/div #js {:className "col-md-2"} (format/format-roles (:roles user)))
               (dom/div #js {:className "col-md-1"} (name (:type user)))
               (dom/div #js {:className "col-md-1"} (ldom/profile-img user :size :small))))))

(defn user-type-combo [{:keys [type] :as user} owner opts]
  (om/component
   (dom/div #js {:className "omlab-label"}
            (dom/span #js {:className "omlab-label"} "Type:")
            (dom/select #js {:className "form-control input-medium"
                             :value (if type (name type) "")
                             :onChange (update-value! user :type keyword)}
                        (into-array
                         (map #(dom/option #js {:value (name %) :key %} (name %))
                              (:user-types opts)))))))

(defn user-edit [{:keys [username name email type roles] :as user} owner _]
  (reify
    om/IWillMount
    (will-mount [_]
      (om/set-state! owner :roles (format/format-roles (:roles user))))

    om/IRenderState
    (render-state [_ {:keys [comm] :as state}]
      (let [valid (and (not (false? (:password-valid user)))
                       (not (util/has-blank-strings? user :username :email :name :type))
                       (not (empty? (:roles user))))]
        (dom/div #js {:className "well well-thin"}
                 (dom/h3 nil (str "User: " username))
                 (dom/div #js {:className "row"}
                          (dom/div #js {:className "col-md-6"}
                                   (dom/ul #js {:className "omlab-field"}
                                           (ldom/editable-label "Name:" user :name)
                                           (ldom/editable-label "E-mail:" user :email
                                                                :on-change-fn #(handler/handle-email % user owner state))
                                           (om/build user-type-combo user
                                                     {:opts {:user-types [:application :operator :system]}})
                                           (ldom/editable-set-label "Roles:" user :roles state :on-change-fn #(handler/handle-roles % user owner state)))
                                   (om/build password-fields user {:init-state {:comm comm}})
                                   (dom/div #js {:className "btn-toolbar"}
                                            (ldom/glyph-button #js {:className "btn btn-primary"
                                                                    :type "button"
                                                                    :disabled (not valid)
                                                                    :onClick #(put! comm [:save-user username])}
                                                               "glyphicon-edit"
                                                               "Save")
                                            (ldom/glyph-button #js {:className "btn btn-default"
                                                                    :onClick #(put! comm [:cancel-user-editing username])}
                                                               "glyphicon-ban-circle" "Cancel")))
                          (dom/div #js {:className "col-md-6"}
                                   (ldom/profile-img user :size :big))))))))

(defn user-show [app owner _]
  (reify
    om/IWillMount
    (will-mount [_]
      (let [username (-> app :admin :showing-user)
            comm (om/get-state owner :comm)]
        (go (let [detail (<! (fetch-user-detail app (str "/admin/user/" username)))]
              (om/transact! app [:users username] #(merge % detail {:detail :loaded}))))))

    om/IRenderState
    (render-state [_ {:keys [comm] :as state}]
      (let [user (get-in app [:users (-> app :admin :showing-user)])
            username (:username user)]
        (dom/div #js {:className "well well-thin"}
                 (dom/h3 nil (str "User: " username))
                 (dom/div #js {:className "row"}
                          (dom/div #js {:className "col-md-6"}
                                   (dom/ul #js {:className "omlab-field"}
                                           (ldom/omlab-label "Name:" (:name user))
                                           (ldom/omlab-label "E-mail:" (:email user))
                                           (ldom/omlab-label "Type:" (some-> user :type name))
                                           (ldom/omlab-label "Roles:" (format/format-roles (:roles user))))
                                   (ldom/glyph-button #js {:className "btn btn-primary"
                                                           :type "button"
                                                           :disabled (not= (:detail user) :loaded)
                                                           :onClick #(put! comm [:edit-user username])}
                                                      "glyphicon-edit"
                                                      "Edit"))
                          (dom/div #js {:className "col-md-6"}
                                   (ldom/profile-img user :size :big))))))))

(defn show-user-list [app init-state]
  (dom/div #js {:className "col-md-10"}
           (dom/div #js {:className "row"}
                    (apply dom/div #js {:className "col-md-12"}
                           (users-header app)
                           (om/build-all user-short
                                         (->> (:users app)
                                              (vals)
                                              (sort-by :name))
                                         (assoc init-state :key guid))))
           (dom/div nil
                    (ldom/glyph-button-link-standalone #js {:className "btn btn-primary btn-bottom-spacing"
                                                                 :href "#/admin/users/add"}
                                                            "glyphicon-plus-sign"
                                                            "Add New User"))))

(defn show-user-edit [app init-state]
  (dom/div #js {:className "col-md-10"}
           (when-let [user (get-in app [:users (-> app :admin :showing-user)])]
             (if (:editing user)
               (om/build user-edit user init-state)
               (om/build user-show app init-state)))))

(defn user-add [user owner opts]
  (reify
    om/IWillMount
    (will-mount [_]
      (om/update! user {:type :operator :roles #{:user}})
      (om/set-state! owner :roles "user"))

    om/IRenderState
    (render-state [_ {:keys [comm] :as state}]
      (let [valid (and (not (false? (:password-valid user)))
                       (not (util/has-blank-strings? user :username :email :name :type))
                       (not (empty? (:roles user))))]
        (dom/div #js {:className "col-md-10"}
                 (dom/div #js {:className "well well-thin"}
                          (dom/h3 nil "Add New User")
                          (dom/div #js {:className "row"}
                                   (dom/div #js {:className "col-md-6"}
                                            (dom/ul #js {:className "omlab-field"}
                                                    (ldom/editable-label "Username:" user :username)
                                                    (ldom/editable-label "Name:" user :name)
                                                    (ldom/editable-label "E-mail:" user :email
                                                                         :on-change-fn #(handler/handle-email % user owner state))
                                                    (om/build user-type-combo user
                                                              {:opts {:user-types [:application :operator :system]}})
                                                    (ldom/editable-set-label "Roles:" user :roles state :on-change-fn #(handler/handle-roles % user owner state)))
                                            (om/build password-fields user {:init-state {:comm comm}})
                                            (dom/div #js {:className "btn-toolbar"}
                                                     (ldom/glyph-button #js {:className "btn btn-primary"
                                                                             :type "button"
                                                                             :disabled (not valid)
                                                                             :onClick #(put! comm [:add-user @user])}
                                                                        "glyphicon-edit"
                                                                        "Save")
                                                     (ldom/glyph-button-link #js {:className "btn btn-default"
                                                                                  :href "#/admin/users"}
                                                                             "glyphicon-ban-circle" "Cancel")))
                                   (dom/div #js {:className "col-md-6"}
                                            (ldom/profile-img user :size :big)))))))))

(defn sidebar-menuitem [{:keys [text] :as c}]
  (dom/li #js {:className "list-group-item"} text))

(defn admin-sidebar [app]
  (om/component
   (dom/div #js {:className "panel panel-default"}
            (dom/div #js {:className "panel-heading"}
                     (dom/b nil "Section"))
            (dom/ul #js {:className "list-group"}
                    (sidebar-menuitem {:text "Users"})))))

(defn admin [app owner _]
  (reify
    om/IWillMount
    (will-mount [_]
      (let [comm (om/get-state owner :comm)]
        (go (let [users (<! (fetch-users app "/admin/users"))]
              (om/update! app [:users] users)))))

    om/IRenderState
    (render-state [_ {:keys [comm]}]
      (let [init-state {:init-state {:comm comm}}]
        (dom/div #js {:className "row"}
                 (dom/div #js {:className "col-md-2"}
                          (om/build admin-sidebar app {:init-state {:comm comm}}))
                 (condp = (-> app :admin :action)
                   :list (show-user-list app init-state)
                   :show (show-user-edit app init-state)
                   :add (om/build user-add (:new-user app) init-state)))))))

(defn profile-panel [user-profile owner state comm]
  (let [valid (and (not (false? (:password-valid user-profile)))
                   (not (util/has-blank-strings? user-profile :email :name :old-password)))]
    (dom/div #js {:className "row"}
             (dom/div #js {:className "col-md-6"}
                      (dom/ul #js {:className "omlab-field"}
                              (ldom/editable-label "Name:" user-profile :name)
                              (ldom/editable-label "E-mail:" user-profile :email
                                                   :on-change-fn #(handler/handle-email % user-profile owner state)))
                      (dom/ul #js {:className (str "omlab-field" (when (:old-password-invalid user-profile) " has-error"))}
                              (dom/li nil
                                      (dom/label #js {:className "omlab-label"}
                                                 (dom/span #js {:className "omlab-label"} "Current Password")
                                                 (dom/input #js {:className "form-control input-medium"
                                                                 :type "password" :value (:old-password user-profile)
                                                                 :onChange (fn [e]
                                                                             (let [nval (.. e -target -value)]
                                                                               (om/update! user-profile :old-password nval)
                                                                               (om/update! user-profile :old-password-invalid false)))}))))
                      (om/build password-fields user-profile {:init-state {:comm comm}})
                      (dom/div #js {:className "btn-toolbar"}
                               (ldom/glyph-button #js {:className "btn btn-primary"
                                                       :type "button"
                                                       :disabled (not valid)
                                                       :onClick #(put! comm [:save-user-profile @user-profile])}
                                                  "glyphicon-edit"
                                                  "Save")))
             (dom/div #js {:className "col-md-6"}
                      (ldom/profile-img user-profile :size :big)))))

(defn profile-edit [app owner _]
  (reify
    om/IWillMount
    (will-mount [_]
      (load-profile app))

    om/IRenderState
    (render-state [_ {:keys [comm] :as state}]
      (let [user-profile (:user-profile app)]
        (dom/div #js {:className "row"}
                 (dom/div #js {:className "col-md-8 col-md-offset-2"}
                          (dom/div #js {:className "well well-thin"}
                                   (dom/h3 nil (str "Profile: " (:username user-profile)))
                                   (profile-panel user-profile owner state comm))))))))
