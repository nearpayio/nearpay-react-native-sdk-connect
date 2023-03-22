import { NativeModules, Platform } from 'react-native';
const LINKING_ERROR = `The package 'react-native-nearpay-plugin' doesn't seem to be linked. Make sure: \n\n` + Platform.select({
  ios: "- You have run 'pod install'\n",
  default: ''
}) + '- You rebuilt the app after installing the package\n' + '- You are not using Expo Go\n';
const NearpayPlugin = NativeModules.NearpayPlugin ? NativeModules.NearpayPlugin : new Proxy({}, {
  get() {
    throw new Error(LINKING_ERROR);
  }
});
function initialize(inputParams) {
  return NearpayPlugin.initialize(inputParams);
}
function purchase(inputParams) {
  return NearpayPlugin.purchase(inputParams);
}
function refund(inputParams) {
  return NearpayPlugin.refund(inputParams);
}
function reconcile(inputParams) {
  return NearpayPlugin.reconcile(inputParams);
}
function reverse(inputParams) {
  return NearpayPlugin.reverse(inputParams);
}
function logout() {
  return NearpayPlugin.logout();
}
function setup() {
  return NearpayPlugin.setup();
}
function session(inputParams) {
  return NearpayPlugin.session(inputParams);
}
function receiptToImage(inputParams) {
  return NearpayPlugin.recieptToImage(inputParams);
}
var Environments;
(function (Environments) {
  Environments["sandbox"] = "sandbox";
  Environments["testing"] = "testing";
  Environments["production"] = "production";
})(Environments || (Environments = {}));
var AuthenticationType;
(function (AuthenticationType) {
  AuthenticationType["login"] = "userenter";
  AuthenticationType["email"] = "email";
  AuthenticationType["mobile"] = "mobile";
  AuthenticationType["jwt"] = "jwt";
})(AuthenticationType || (AuthenticationType = {}));
var Locale;
(function (Locale) {
  Locale["default"] = "default";
})(Locale || (Locale = {}));
export { Locale, AuthenticationType, Environments, setup, logout, reverse, reconcile, refund, purchase, initialize, session, receiptToImage };
//# sourceMappingURL=index.js.map