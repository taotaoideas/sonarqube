define([
  'workspace/views/item-view',
  'templates/workspace'
], function (ItemView) {

  return Marionette.CompositeView.extend({
    className: 'workspace-nav',
    template: Templates['workspace-items'],
    itemViewContainer: '.workspace-nav-list',
    itemView: ItemView,

    itemViewOptions: function () {
      return { collectionView: this };
    }
  });

});
