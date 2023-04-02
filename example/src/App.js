import React, { Component, Props } from 'react';

import { Button, StyleSheet, View, } from 'react-native';
import * as Nearpay from 'react-native-nearpay-plugin';
import Toast from 'react-native-toast-message';

let authValue = "nearpay@nearpay.io";
// userenter,email,mobile,jwt
let authType = Nearpay.AuthenticationType.email;
//Time out n seconds
let timeout = "60";
export default class App extends Component {


  constructor(props){  
    super(props); 
    this.state = {
      loggedIn: false,
      currentState: "not-panic",

    }
    var reqData = {
      "authtype" : authType,
      "authvalue" : authValue,
      "locale" : Nearpay.Locale.default,
      "environment" : Nearpay.Environments.sandbox
      };
      Nearpay.initialize(reqData).then((response) => {
      let resultJSON = JSON.parse(response)
      console.log(resultJSON.message,",,,,data....",resultJSON.status);
      if(resultJSON.status == 200){
        this.showToast('success','initializ Success',resultJSON.message )
      }else{
        this.showToast('error','initializ Failed',resultJSON.message )
      }

    });
  }

  showToast(type,title, message){
    Toast.show({
      type: type,
      text1: title,
      text2: message
    });
  }

  initiatePurchase(){
    var reqData = {
      "amount": "0001", // Required
      "customer_reference_number": "uuyuyuyuy65565675", // [optional] any number you want to add as a refrence
      "isEnableUI" : true, // [optional] true will enable the ui and false will disable 
      "isEnableReversal" : true, // it will allow you to enable or disable the reverse button
      "finishTimeout" : timeout // [optional] Add the number of seconds      
    };
    console.log("initializePayment","response");
    Nearpay.purchase(reqData).then((response) => {
      let resultJSON = JSON.parse(response);
      console.log(resultJSON,"initializePayment",resultJSON.message);
      if(resultJSON.status == 200){
        this.showToast('success','Purchase Success',resultJSON.messsage );
      }else{
        this.showToast('error','Purchase Failed',resultJSON.message );
      }

    });
  }

  initiatePurchaseAndRefund( ){
    var reqData = {
      "amount": "0001", 
      "customer_reference_number": "", // Any string as a reference number
      "isEnableUI" : true, // Optional
      "isEnableReversal" : true, // Optional it will allow you to enable or disable the reverse button
      "finishTimeout" : timeout //Optional 
    };
    Nearpay.purchase(reqData).then((response) => {
      var responseJson = JSON.parse(response);
      var status = responseJson.status;
      var message = responseJson.message;
      if(status == 200){
          var purchaseList = responseJson.list;
          setTimeout(() => {
            console.log(purchaseList.length,"paymentresponse",purchaseList);
            if(purchaseList.length){
              let uuid = purchaseList[0].uuid;
              console.log("response list", uuid);
              this.initiateRefund(uuid);
            }
          }, 5000);
      }else{
        this.showToast('error','Purchase Failed',responseJson.message );
      }
      console.log("initialisePayment",response);
    });
  }

  initiateRefund(uuid){
    var reqData = {
      "amount": "0001", // Required
      "transaction_uuid" :uuid, // Required
      "customer_reference_number": "rerretest123333333", //Optional
      "isEnableUI" : true, // Optional
      "isEnableReversal" : true,// Optional
      "isEditableReversalUI" : true,// Optional
      "finishTimeout" : timeout, // Optional
      "adminPin" : "0000", // [optional] when you add the admin pin here , the UI for admin pin won't be shown.

    };
    Nearpay.refund(reqData).then((response) => {
      console.log("initializePayment",response);
      var resultJSON = JSON.parse(response);
      if(resultJSON.status == 200){
        this.showToast('success','Refund Success',resultJSON.message )
      }else{
        this.showToast('error','Refund Failed',resultJSON.message )
      }
    });
  }

  initiateReconcile(){
    var reqData = {
      "isEnableUI" : true,// Optional
      "finishTimeout" : timeout,    // Optional
      //"adminPin" : "0000", // [optional] when you add the admin pin here , the UI for admin pin won't be shown.
    };
    Nearpay.reconcile(reqData).then((response) => {
      console.log("initialisePayment",response);
      var resultJSON = JSON.parse(response);
      if(resultJSON.status == 200){
        this.showToast('success','Reconcile Success',resultJSON.message )
      }else{
        this.showToast('error','Reconcile Failed',resultJSON.message )
      }
    });
  }

  initiatePurchaseAndReverse( ){

    var reqData = {
      "amount": "0001", // Required
      "customer_reference_number": "uuyuyuyuy65565675", // Optional
      "isEnableUI" : true, //Optional
      "isEnableReversal" : true, //it will allow you to enable or disable the reverse button
      "finishTimeout" : timeout //Optional
    };

    Nearpay.purchase(reqData).then((response) => {
      print("....initiatePurchaseAndReverse....$response",response);
      var responseJson = JSON.parse(response);
      var status = responseJson.status;
      var message = responseJson.message;
      if(status == 200){
          var purchaseList = responseJson.list;
          setTimeout(() => {
            console.log(purchaseList.length,"paymentresponse",purchaseList);
            if(purchaseList.length){
              let uuid = purchaseList[0].uuid;
              console.log("...response list...uuid------$uuid..333..", uuid);
              this.doReverse(uuid);
            }
          }, 5000);
      }else{
        this.showToast('error','Purchase Failed',responseJson.message );
      }
      console.log(".....initialisePayment......44.......",response);
    });
  }


  doReverse(uuid){
    var reqData = {
      "transaction_uuid" :uuid, // Required
      "isEnableUI" : true, // Optional
      "finishTimeout" : timeout   // Optional
    };
    Nearpay.reverse(reqData).then((response) => {
      console.log("initialisePayment",response);
      var resultJSON = JSON.parse(response);
      if(resultJSON.status == 200){
        this.showToast('success','Reverse Success',resultJSON.message )
      }else{
        this.showToast('error','Reverse Failed',resultJSON.message )
      }
    });
  }

  doLogout(){
    Nearpay.logout().then((response) => {
      console.log("doLogoutAction",response);
      var resultJSON = JSON.parse(response);
      console.log("doLogoutAction",resultJSON.message);
      if(resultJSON.status == 200){
        this.showToast('success','Logout Success',resultJSON.message )
      }else{
        this.showToast('error','Logout Failed',resultJSON.message )
      }
    });
  }

  doSetupClick(){
    Nearpay.setup().then((response) => {
      console.log("doSetupClick",response);
      var resultJSON = JSON.parse(response);
      console.log("doSetupClick",resultJSON.messsage);
      if(resultJSON.status == 200){
        this.showToast('success','Setup Success',resultJSON.messsage )
      }else{
        this.showToast('error','Setup Failed',resultJSON.messsage )
      }
    });
  }

  render() {
    return (
      <View style={styles.container}>
            <View style={styles.containerrow}>
                <Button title="Purchase" onPress={()=>this.initiatePurchase()}/>
              </View>
              <View style={styles.containerrow}>
                <Button title="Purchase and Refund " onPress={()=>this.initiatePurchaseAndRefund()}/>
              </View>
              <View style={styles.containerrow}>
                <Button title="Purchase and Reverse " onPress={()=>this.initiatePurchaseAndReverse()}/>
              </View>
              <View style={styles.containerrow}>
                <Button title="Reconcile" onPress={()=>this.initiateReconcile()}/>
              </View>
              <View style={styles.containerrow} >
                <Button title="Setup" onPress={()=>this.doSetupClick()} />
              </View>
              <View style={styles.containerrow} >
                <Button title="Logout" onPress={()=>this.doLogout()} />
              </View>
              <View style={styles.containerrow} >
                <Button title="Session" onPress={()=>this.doSession()} />
              </View>
              <View style={styles.containerrow} >
                <Button title="Connect" onPress={()=>this.doConnect()} />
              </View>
              <View style={styles.containerrow} >
                <Button title="Show" onPress={()=>this.doShow()} />
              </View>
              <View style={styles.containerrow} >
                <Button title="Get Connect Session" onPress={()=>this.doGetSession()} />
              </View>
              <View style={styles.containerrow} >
                <Button title="Disconnect" onPress={()=>this.doDisconnect()} />
              </View>
            <Toast />

            </View>
    )
  }

  doSession(){
    var reqData = {
      "sessionID" :"ea5e30d4-54c7-4ad9-8372-f798259ff589", // Required
      //"isEnableUI" : true, //Optional
      //"isEnableReversal" : true, 
      //"finishTimeout" : timeout  // Optional
    };
    Nearpay.session(reqData).then((response) => {
      console.log("doSession",response);
      var resultJSON = JSON.parse(response);
      if(resultJSON.status == 200){
        this.showToast('success','Session Success',resultJSON.message )
      }else{
        this.showToast('error','Session Failed',resultJSON.message )
      }
    });
  }

  doConnect(){
    var reqData = {
      "port" :8080
    };
    Nearpay.connect(reqData).then((response) => {
      console.log("doConnect",response);
      var resultJSON = JSON.parse(response);
      if(resultJSON.status == 200){
        this.showToast('success','Session Success',resultJSON.message )
      }else{
        this.showToast('error','Session Failed',resultJSON.message )
      }
    });
  }

  doShow(){
    Nearpay.showConnection().then((response) => {
      console.log("doShow",response);
      var resultJSON = JSON.parse(response);
      if(resultJSON.status == 200){
        this.showToast('success','Session Success',resultJSON.message )
      }else{
        this.showToast('error','Session Failed',resultJSON.message )
      }
    });
  }

  doDisconnect(){
    Nearpay.connectionDisconnect().then((response) => {
      console.log("disConnect",response);
      var resultJSON = JSON.parse(response);
      if(resultJSON.status == 200){
        this.showToast('success','Session Success',resultJSON.message )
      }else{
        this.showToast('error','Session Failed',resultJSON.message )
      }
    });
  }

  doGetSession(){
    Nearpay.getConnectionSession().then((response) => {
      console.log("getSession",response);
      var resultJSON = JSON.parse(response);
      if(resultJSON.status == 200){
        this.showToast('success','Session Success',resultJSON.message )
      }else{
        this.showToast('error','Session Failed',resultJSON.message )
      }
    });
  }

}

const styles = StyleSheet.create({

  box: {
    width: 60,
    height: 60,
    marginVertical: 20,
  },
  container: {
    flex: 1,
    flexDirection: 'column',
    alignItems: 'center',
    justifyContent: 'center',
},
containerrow: {
  flexDirection: 'row',
  //marginBottom : 10,
  marginBottom:'5%'
},
buttonContainer: {
    flex: 1,
}
});
