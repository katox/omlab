(ns omlab.handler-test
  (:require  [clojure.test :refer [deftest testing is]]
             [ring.mock.request :refer [request]]
             [omlab.handler :refer :all]))

(deftest test-app
  (testing "sign in form"
    (let [response (app (request :get "/login"))]
      (is (= 200 (:status response)))))
  
  (testing "not-found route"
    (let [response (app (request :get "/invalid"))]
      (is (= 404 (:status response)))))

  (testing "accessible web assets"
    (let [response (app (request :get "/css/omlab.css"))]
      (is (= 200 (:status response)))))
  
  (testing "unauthorized ui requests"
    (let [response (app (request :get "/ui/cmd"))]
      (is (= 401 (:status response))))))
