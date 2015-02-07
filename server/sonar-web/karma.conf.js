// Karma configuration
// Generated on Sat Feb 07 2015 12:23:32 GMT+0100 (CET)

module.exports = function (config) {
  config.set({

    // base path that will be used to resolve all patterns (eg. files, exclude)
    basePath: '',


    // frameworks to use
    // available frameworks: https://npmjs.org/browse/keyword/karma-adapter
    frameworks: ['jasmine'],


    // list of files / patterns to load in the browser
    files: [
      'src/main/webapp/js/sonar.js',
      'src/main/webapp/js/require.js',

      { pattern: 'src/main/js/**/*.js', included: false },
      { pattern: 'src/main/webapp/js/**/*.js', included: false },

      //{ pattern: 'tests/unit/**/*.js', included: false },

      'src/main/test-main.js'
    ],


    // list of files to exclude
    exclude: [
      '**/webapp/js/tests/**/*',
      '**/tests/e2e/**/*'
    ],


    // preprocess matching files before serving them to the browser
    // available preprocessors: https://npmjs.org/browse/keyword/karma-preprocessor
    preprocessors: {
      '**/coding-rules/**/*': 'coverage'
    },


    // test results reporter to use
    // possible values: 'dots', 'progress'
    // available reporters: https://npmjs.org/browse/keyword/karma-reporter
    reporters: ['progress', 'coverage'],


    coverageReporter: {
      dir: 'target',
      subdir: 'karma',
      //file: 'coverage.lcov',
      type: 'lcovonly'
    },


    // web server port
    port: 9876,


    // enable / disable colors in the output (reporters and logs)
    colors: true,


    // level of logging
    // possible values: config.LOG_DISABLE || config.LOG_ERROR || config.LOG_WARN || config.LOG_INFO || config.LOG_DEBUG
    logLevel: config.LOG_INFO,


    // enable / disable watching file and executing tests whenever any file changes
    autoWatch: false,


    // start these browsers
    // available browser launchers: https://npmjs.org/browse/keyword/karma-launcher
    browsers: ['PhantomJS'],


    // Continuous Integration mode
    // if true, Karma captures browsers, runs the tests and exits
    singleRun: true
  });
};
