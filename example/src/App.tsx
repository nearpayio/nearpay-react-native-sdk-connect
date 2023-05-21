import React from 'react';
import { NavigationContainer } from '@react-navigation/native';

import ProxySide from './components/ProxySide';
import EmbededSide from './components/EmbededSide';
import { createNativeStackNavigator } from '@react-navigation/native-stack';
import Home from './components/Home';

const Stack = createNativeStackNavigator();

export default function App() {
  return (
    <NavigationContainer>
      <Stack.Navigator>
        <Stack.Screen
          name="Home"
          component={Home}
          options={{ title: 'Welcome to Nearpay SDK' }}
        />
        <Stack.Screen
          name="Embeded"
          component={EmbededSide}
          options={{ title: 'Welcome' }}
        />
        <Stack.Screen name="Proxy" component={ProxySide} />
      </Stack.Navigator>
    </NavigationContainer>
  );
}
