import { createStaticNavigation } from '@react-navigation/native';
import { createBottomTabNavigator } from '@react-navigation/bottom-tabs';
import Ionicons from '@react-native-vector-icons/ionicons';
import { ComponentProps } from 'react';
import { HomeView } from '../views/home/home.view';
import { CalendarView } from '../views/calendar/calendar.view';

const RootStack = createBottomTabNavigator({
  screenOptions: ({ route }) => ({
    tabBarIcon: ({ focused, color, size }) => {
      let iconName: ComponentProps<typeof Ionicons>['name'];

      switch (route.name) {
        case 'Home':
          iconName = focused ? 'apps' : 'apps-outline';
          break;
        case 'Calendar':
          iconName = focused ? 'calendar' : 'calendar-outline';
        default:
          iconName = 'ellipse';
      }

      // You can return any component that you like here!
      return <Ionicons name={iconName} size={size} color={color} />;
    },
    tabBarActiveTintColor: 'tomato',
    tabBarInactiveTintColor: 'gray',
    headerShown: false,
  }),
  screens: {
    Home: HomeView,
    Calendar: CalendarView,
  },
});

export const Navigation = createStaticNavigation(RootStack);
