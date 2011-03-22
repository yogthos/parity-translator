(ns translate
  (:require [org.danlarkin.json :as json])
  (:import  
    (java.net URLEncoder)    
    (org.apache.http HttpResponse)    
    (org.apache.http.client.methods HttpGet)    
    (org.apache.http.impl.client DefaultHttpClient)
    (org.apache.http.protocol BasicHttpContext HttpContext)
    (org.apache.http.util EntityUtils)))

(defn init-client [url]
  (let [client  (new DefaultHttpClient)
        context (new BasicHttpContext)]
                  
    (defn get-response [http-get]
      (let [http-response (.execute client http-get context)]
        (when (not= 200 (.. http-response getStatusLine getStatusCode))
          (throw (new Exception (str "Request failed" (.. http-response getStatusLine toString)))))
       
        (EntityUtils/toString (.getEntity http-response))))
   
    (defn send-query [source, target, text]     
      (let [query (str url
                    "?key=YOUR_KEY"                     
                    "&source=" source
                    "&target=" target
                    "&q=" (URLEncoder/encode text "UTF-8"))
            http-get (new HttpGet query)]       
        (->> (get-response http-get)
          json/decode-from-str
          :data
          :translations
          first
          :translatedText)))))
 

(defn translate [langs text]
  (reduce (fn [cur-text [from to]]            
            (send-query from to cur-text))
    text (partition 2 1 langs)))

(defn -main [text from to & more]
  (init-client "https://www.googleapis.com/language/translate/v2")
  (let [langs (concat [from to] more [from])]
    (println "translating through:" langs)
    (loop [last-text text
           cur-text (translate langs text)]
      (if (= last-text cur-text)
        (println "\noriginal:  " text
          "\ntranslated:" cur-text)
        (do
          (println "\nbefore:" last-text
            "\nafter: "  cur-text)
          (recur cur-text (translate langs cur-text)))))))

;(-main "Tear-drinking moths, probably the world's biggest assholes, sneak up on larger animals and poke them in the eyes until they cry so they can drink the tears." "en" "de" "fr")
