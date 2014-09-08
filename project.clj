(defproject tortoise-cljs "0.0.1-SNAPSHOT"
  :plugins [[lein-cljsbuild "1.0.3"]
            [lein-ring "0.8.11"]]
  :description "tortoise in clojurescript"
  :dependencies [[org.clojure/clojure "1.6.0"]
                 [org.clojure/clojurescript "0.0-2268"]]
  :cljsbuild {
    :repl-listen-port 9000
    :repl-launch-commands
      {"chromium" ["chromium-browser" "http://localhost:9000"]}
    :builds {
      :default {
        :source-paths ["src/main/cljs/"]
        :compiler {
          :output-to "target/classes/js/tortoise-engine-cljs.js"
          :optimizations :whitespace
          :pretty-print true
          :libs ["./target/classes/js/tortoise/shim/lodash-cl.js"
                 "./target/classes/js/tortoise/shim/cloner-cl.js"
                 "./target/classes/js/tortoise/shim/strictmath-cl.js"
                 "./target/classes/js/tortoise/shim/random-cl.js"] }}
      :benchmark {
        :source-paths ["src/main/cljs" "src/test/cljs"]
        :compiler {
          :output-to "target/classes/js/tortoise-engine-cljs-bench.js"
          :optimizations :whitespace
          :pretty-print true
          :libs ["./target/classes/js/tortoise/shim/lodash-cl.js"
                 "./target/classes/js/tortoise/shim/cloner-cl.js"
                 "./target/classes/js/tortoise/shim/strictmath-cl.js"
                 "./target/classes/js/tortoise/shim/random-cl.js"] }}}})