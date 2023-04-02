declare function initialize(inputParams: any): Promise<String>;
declare function purchase(inputParams: any): Promise<String>;
declare function refund(inputParams: any): Promise<String>;
declare function reconcile(inputParams: any): Promise<String>;
declare function reverse(inputParams: any): Promise<String>;
declare function logout(): Promise<String>;
declare function setup(): Promise<String>;
declare function session(inputParams: any): Promise<String>;
declare function receiptToImage(inputParams: any): Promise<String>;
declare enum Environments {
    sandbox = "sandbox",
    testing = "testing",
    production = "production"
}
declare enum AuthenticationType {
    login = "userenter",
    email = "email",
    mobile = "mobile",
    jwt = "jwt"
}
declare enum Locale {
    default = "default"
}
export { Locale, AuthenticationType, Environments, setup, logout, reverse, reconcile, refund, purchase, initialize, session, receiptToImage };
//# sourceMappingURL=index.d.ts.map