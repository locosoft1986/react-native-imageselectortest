'use strict';

var React = require('react-native');
var RCTImageSelectorManager = React.NativeModules.ImageSelectorManager;

var ImageSelectorManager = {
  launch: function(options, callback) {
  	options = options || {};
  	callback = callback || (function() {});

  	var params = {
			singleMode: options.singleMode || false,
			showCamera: options.showCamera || true,
			max: options.max || 9
  	};
		console.log(`options: ${params}`);
		RCTImageSelectorManager.launch(params, callback);
  }
};

module.exports = ImageSelectorManager;
