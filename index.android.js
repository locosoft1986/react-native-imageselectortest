/**
 * Sample React Native App
 * https://github.com/facebook/react-native
 */
'use strict';
import React, {
  AppRegistry,
  Component,
  StyleSheet,
  Text,
  View,
  Platform,
  TouchableHighlight,
  TouchableNativeFeedback
} from 'react-native';

import ImageSelectorManager from './android/imageselector/imageSelector';

class ImagePickerApp extends Component {
  constructor(props) {
    super(props);

    this.state = {
      imageUrls: []
    }
  }

  onClicked = () => {
    ImageSelectorManager.launch({singleMode: false}, (canceled, urls) => {
      console.log(`Urls: ${urls}`);
      if(!canceled) {
        this.setState({imageUrls: urls});
      }
    });
  };

  renderImageUrls() {
    let {imageUrls} = this.state;
    return imageUrls.map((url, index) => {
      return (<Text key={index}>{url}</Text>)
    });
  }

  render() {
    let TouchableElement = TouchableHighlight;
    if (Platform.OS === 'android') {
      TouchableElement = TouchableNativeFeedback;
    }

    return (
      <View style={styles.container}>
        <Text style={styles.welcome}>
          Welcome to React Native!
        </Text>
        <TouchableElement
          style={styles.button}
          onPress={this.onClicked}>
          <View>
            <Text>Choose Pictures</Text>
          </View>
        </TouchableElement>
        {this.renderImageUrls()}
      </View>
    );
  }
}

const styles = StyleSheet.create({
  container: {
    flex: 1,
    justifyContent: 'center',
    alignItems: 'center',
    backgroundColor: '#F5FCFF'
  },
  welcome: {
    fontSize: 20,
    textAlign: 'center',
    margin: 10,
  },
  instructions: {
    textAlign: 'center',
    color: '#333333',
    marginBottom: 5
  },
  button: {
    textAlign: 'center',
    color: '#ffffff',
    marginBottom: 7,
    borderWidth: 1,
    borderColor: 'blue',
    borderStyle: 'solid',
    borderRadius: 2
  }
});

AppRegistry.registerComponent('ImagePickerApp', () => ImagePickerApp);
