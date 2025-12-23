import { StyleSheet } from 'react-native';

export const styles = StyleSheet.create({
  calendar: {
    paddingLeft: 20,
    paddingRight: 20,
  },
  header: {
    flexDirection: 'row',
    justifyContent: 'center',
    alignItems: 'center',
    marginVertical: 10,
  },
  headerTitle: { fontSize: 16, fontWeight: 'bold', marginRight: 6 },
  section: {
    backgroundColor: 'white',
    color: 'grey',
    textTransform: 'capitalize',
  },
});
