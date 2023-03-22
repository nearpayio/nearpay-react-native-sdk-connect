"use strict";

Object.defineProperty(exports, "__esModule", {
  value: true
});
exports.Locale = exports.Environments = exports.AuthenticationType = void 0;
exports.initialize = initialize;
exports.logout = logout;
exports.purchase = purchase;
exports.receiptToImage = receiptToImage;
exports.reconcile = reconcile;
exports.refund = refund;
exports.reverse = reverse;
exports.session = session;
exports.setup = setup;
var _reactNative = require("react-native");
const LINKING_ERROR = `The package 'react-native-nearpay-plugin' doesn't seem to be linked. Make sure: \n\n` + _reactNative.Platform.select({
  ios: "- You have run 'pod install'\n",
  default: ''
}) + '- You rebuilt the app after installing the package\n' + '- You are not using Expo Go\n';
const NearpayPlugin = _reactNative.NativeModules.NearpayPlugin ? _reactNative.NativeModules.NearpayPlugin : new Proxy({}, {
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
exports.Environments = Environments;
(function (Environments) {
  Environments["sandbox"] = "sandbox";
  Environments["testing"] = "testing";
  Environments["production"] = "production";
})(Environments || (exports.Environments = Environments = {}));
var AuthenticationType;
exports.AuthenticationType = AuthenticationType;
(function (AuthenticationType) {
  AuthenticationType["login"] = "userenter";
  AuthenticationType["email"] = "email";
  AuthenticationType["mobile"] = "mobile";
  AuthenticationType["jwt"] = "jwt";
})(AuthenticationType || (exports.AuthenticationType = AuthenticationType = {}));
var Locale;
exports.Locale = Locale;
(function (Locale) {
  Locale["default"] = "default";
})(Locale || (exports.Locale = Locale = {}));
//# sourceMappingURL=index.js.map