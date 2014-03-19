(ns omlab.db.user-test
  (:require [clojure.test :refer [deftest testing is use-fixtures]]
            [conjure.core :refer [mocking stubbing instrumenting] :as c]
            [datomic.api :refer [q] :as d]
            [omlab.test :as t]
            [omlab.util :refer [has-keys?] :as util]
            [omlab.db.migration]
            [omlab.db.util :as db.util]
            [omlab.db.user :refer :all]))

(use-fixtures :once t/settings-fixture)
(use-fixtures :each (t/refresh-db-fixture "test/dtm/user-test-data.edn"))

(def extra-user-tx
  [{:db/id #db/id[:db.part/user]
    :user/username "extra"
    :user/password "xxx"
    :user/email "extra@omlab.user"
    :user/roles #{:omlab.auth/admin :omlab.auth/user}
    :user/type :user.type/system
    :user/name "Extra User"
    :user/optlock 0}])

(deftest user-lookup
  (testing "lookup missing user"
    (is (= nil (find-entity-id (t/db) "missing-user"))))
  (testing "lookup existing user"
    (let [res (find-entity-id (t/db) "root")]
      (is (not= nil res)))))

(deftest auth-creds
  (testing "no auth"
    (is (= nil (auth-credentials (t/db) "missing-user"))))
  (testing "root access"
    (is (= {:username "root", :password  "$2a$10$Hfb7nFYC8s7f7gX5wxsOZ.YvoFe7NVyc4jvOwITNR4ZDLNGOCO8Ci", :roles #{:omlab.auth/admin}, :name "Omlab Administrator"}
           (auth-credentials (t/db) "root"))))
  (testing "multiple roles"
    (is (= nil
           (auth-credentials (:db-before (d/with (t/db) extra-user-tx)) "extra")))
    (is (= {:username "extra", :password "xxx", :roles #{:omlab.auth/admin :omlab.auth/user}, :name "Extra User"}
           (auth-credentials (:db-after (d/with (t/db) extra-user-tx)) "extra")))))

(deftest user-info
  (testing "missing user detail"
    (is (= nil (user-detail (t/db) "missing-user"))))
  (testing "user detail"
    (let [res (user-detail (t/db) "root")]
      (is (map? res))
      (is (has-keys? res :name :roles :username :name :email :type :mod-time))))
  (testing "user listing"
    (let [{:keys [db-before db-after]} (d/with (t/db) extra-user-tx)
          pres (list-users db-before)
          res (list-users db-after)]
      (is (= (inc (count pres)) (count res)))
      (is (every? #(has-keys? % :name :type :roles :email :username) res))
      (is (some #(and (= (:username %) "extra") (= (:roles %) #{:admin :user})) res))
      (is (distinct? (map :username res)))
      (is (= (sort (map :name res)) (map :name res))))))

(deftest user-changes
  (testing "adding user"
    (let [conn (t/conn)
          user {:username "extra"
                :email "extra@omlab.user"
                :name "Extra User"
                :type :system
                :roles #{:admin :user}}
          {:keys [db-before db-after]} (add-user conn {:username "root"} user)]
      (is (= (inc (count (list-users db-before))) (count (list-users db-after))))
      (is (thrown? Exception (add-user conn {:username "root"} user)))))
  (testing "updating user"
    (let [conn (t/conn)
          user {:username "root"
                :email "changed@his.email"
                :name "Rooted!"
                :type :operator
                :roles #{:user}
                :optlock 0}
          {:keys [db-before db-after]} (update-user conn {:username "root"} user)
          pdetail (user-detail db-before "root")
          detail (user-detail db-after "root")]
      (is (= (:username detail) (:username user)))
      (is (= (:email detail) (:email user)))
      (is (= (:name detail) (:name user)))
      (is (= (:type detail) (:type user)))
      (is (= (:roles detail) (:roles user)))
      (is (= (count (list-users db-before)) (count (list-users db-after))))
      (is (= (dissoc detail :mod-time :optlock)
             (dissoc (user-detail (:db-after (update-user conn {:username "root"}
                                                          (update-in user [:optlock] inc)))
                                  "root")
                     :mod-time :optlock))))))

