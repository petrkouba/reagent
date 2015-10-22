(ns reagent.dom
  (:require [cljsjs.react.dom]
            [reagent.impl.util :as util]
            [reagent.impl.template :as tmpl]
            [reagent.debug :refer-macros [dbg]]
            [reagent.interop :refer-macros [$ $!]]))

(defonce ^:private react-dom nil)

(defonce ^:private roots (atom {}))

(defn- dom []
  (if-some [r react-dom]
    r
    (do
      (set! react-dom
            (or (and (exists? js/ReactDOM)
                     js/ReactDOM)
                (and (exists? js/require)
                     (js/require "react-dom"))))
      (assert react-dom "Could not find ReactDOM")
      react-dom)))

(defn- unmount-comp [container]
  (swap! roots dissoc container)
  ($ (dom) unmountComponentAtNode container))

(defn- render-comp [comp container callback]
  (binding [util/*always-update* true]
    (->> ($ (dom) render (comp) container
             (fn []
               (binding [util/*always-update* false]
                 (swap! roots assoc container [comp container])
                 (if (some? callback)
                   (callback))))))))

(defn- re-render-component [comp container]
  (render-comp comp container nil))

(defn render
  "Render a Reagent component into the DOM. The first argument may be
either a vector (using Reagent's Hiccup syntax), or a React element. The second argument should be a DOM node.

Optionally takes a callback that is called when the component is in place.

Returns the mounted component instance."
  ([comp container]
   (render comp container nil))
  ([comp container callback]
   (let [f (fn []
             (tmpl/as-element (if (fn? comp) (comp) comp)))]
     (render-comp f container callback))))

(defn unmount-component-at-node [container]
  (unmount-comp container))

(defn dom-node
  "Returns the root DOM node of a mounted component."
  [this]
  ($ (dom) findDOMNode this))

(set! tmpl/find-dom-node dom-node)

(defn force-update-all
  "Force re-rendering of all mounted Reagent components. This is
  probably only useful in a development environment, when you want to
  update components in response to some dynamic changes to code.

  Note that force-update-all may not update root components. This
  happens if a component 'foo' is mounted with `(render [foo])` (since
  functions are passed by value, and not by reference, in
  ClojureScript). To get around this you'll have to introduce a layer
  of indirection, for example by using `(render [#'foo])` instead."
  []
  (doseq [v (vals @roots)]
    (apply re-render-component v))
  "Updated")
