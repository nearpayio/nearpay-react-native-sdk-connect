# Neearpay react native proxy SDK

## Installation

```
npm install "https://github.com/nearpayio/nearpay-react-native-sdk-connect.git#main" --save

Plugin will support minimum supported ANDROID SDK version 21 and above only.

```

## Initialize

to initialize the proxy use the following:

```typescript
import {
  AuthenticationType,
  EmbededNearpay,
  Environments,
  Locale,
} from 'react-native-nearpay-sdk-proxy';

const nearpayProxy = new EmbededNearpay({
  authtype: AuthenticationType.email,
  authvalue: '<enter your email here>',
  environment: Environments.sandbox,
  locale: Locale.default,
});
```

`EmbededNearpay` obeject should be created once and served to the wholl application

## Methods

proxy gives a set of methods to use, for exmaple



### proxyShowConnection

will show a screen with the current connection if exsists, or a button to start the connection

```typescript
nearpayProxy.proxyShowConnection(); // will show a screen for connection
```

### proxyDisconnect

will disconnect the current pos device

```typescript
nearpayProxy.proxyDisconnect(); // will disconnect current connection
```
