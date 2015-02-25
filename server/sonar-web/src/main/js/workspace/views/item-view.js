define([
  'templates/workspace'
], function () {

  return Marionette.ItemView.extend({
    tagName: 'li',
    className: 'workspace-nav-item',
    template: Templates['workspace-item'],

    events: {
      'click': 'onClick',
      'click .js-close': 'onCloseClick'
    },

    onClick: function (e) {
      e.preventDefault();
      this.options.collectionView.trigger('click', this.model.id, this.model);
    },

    onCloseClick: function (e) {
      e.preventDefault();
      e.stopPropagation();
      this.model.destroy();
    }
  });

});
