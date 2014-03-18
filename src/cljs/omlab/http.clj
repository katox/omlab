(ns omlab.http
  (:require [cljs.core.async.macros :refer [go]]))

(defmacro go-fetch
  "Creates a channel, runs the action and binds its return map as binding.
   Continues with on-success if status is 2xx code or on-error otherwise.

   If either of on-exprs returns a non-nil value it is put on the created channel."
  [[name action] on-success on-error]
  `(let [c# (cljs.core.async/chan)]
     (go
       (let [result# ~action
             ~name result#]
           (some->>
            (if (<= 200 (:status result#) 299)
              ~on-success
              ~on-error)
            (cljs.core.async/>! c#))))
       c#))
