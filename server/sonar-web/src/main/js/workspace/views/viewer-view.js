define([
  'source-viewer/viewer',
  'templates/workspace'
], function (SourceViewer) {

  return Marionette.Layout.extend({
    className: 'workspace-viewer',
    template: Templates['workspace-viewer'],

    regions: {
      viewerRegion: '.workspace-viewer-container'
    },

    events: {
      'click .js-minimize': 'onMinimizeClick',
      'click .js-close': 'onCloseClick'
    },

    onRender: function () {
      this.showViewer();
    },

    onMinimizeClick: function (e) {
      e.preventDefault();
      this.minimize();
    },

    onCloseClick: function (e) {
      e.preventDefault();
      this.close();
    },

    showViewer: function () {
      if (SourceViewer == null) {
        SourceViewer = require('source-viewer/viewer');
      }
      var viewer = new SourceViewer();
      viewer.open(this.model.id);
      viewer.on('loaded', function () {
        viewer.highlightLine(27);
        viewer.scrollToLine(27);
      });
      this.viewerRegion.show(viewer);
    },

    minimize: function () {
      this.trigger('minimize', this.model);
      this.close();
    }
  });

});
