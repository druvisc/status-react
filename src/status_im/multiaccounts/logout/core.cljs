(ns status-im.multiaccounts.logout.core
  (:require [re-frame.core :as re-frame]
            [status-im.chaos-mode.core :as chaos-mode]
            [status-im.i18n :as i18n]
            [status-im.init.core :as init]
            [status-im.native-module.core :as status]
            [status-im.transport.core :as transport]
            [status-im.utils.fx :as fx]
            [status-im.ui.screens.navigation :as navigation]
            [status-im.utils.keychain.core :as keychain]))

(fx/defn logout-method [{:keys [db] :as cofx} auth-method]
  (let [key-uid (get-in db [:multiaccount :key-uid])]
    (fx/merge cofx
              {::logout                      nil
               :keychain/clear-user-password key-uid
               ::init/open-multiaccounts     #(re-frame/dispatch [::init/initialize-multiaccounts % {:logout? true}])}
              (keychain/save-auth-method key-uid auth-method)
              (transport/stop-whisper)
              (chaos-mode/stop-checking)
              (init/initialize-app-db))))

(fx/defn logout
  {:events [:logout]}
  [cofx]
  (logout-method cofx keychain/auth-method-none))

(fx/defn show-logout-confirmation [_]
  {:ui/show-confirmation
   {:title               (i18n/label :t/logout-title)
    :content             (i18n/label :t/logout-are-you-sure)
    :confirm-button-text (i18n/label :t/logout)
    :on-accept           #(re-frame/dispatch [:multiaccounts.logout.ui/logout-confirmed])
    :on-cancel           nil}})

(fx/defn biometric-logout
  {:events [:biometric-logout]}
  [cofx]
  (fx/merge cofx
            (logout-method keychain/auth-method-biometric-prepare)
            (fn [{:keys [db]}]
              {:db (assoc-in db [:multiaccounts/login :save-password?] true)})))

(re-frame/reg-fx
 ::logout
 (fn []
   (status/logout)))
