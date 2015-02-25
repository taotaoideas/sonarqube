define([
  'workspace/models/item',
  'workspace/models/items',
  'workspace/views/items-view',
  'workspace/views/viewer-view'
], function (Item, Items, ItemsView, ViewerView) {

  var instance = null,

      Workspace = function () {
        if (instance != null) {
          throw new Error('Cannot instantiate more than one Workspace, use Workspace.getInstance()');
        }
        this.initialize();
      };

  Workspace.prototype = {
    initialize: function () {
      var that = this;

      this.items = new Items();
      this.items.load();

      this.itemsView = new ItemsView({ collection: this.items });
      this.itemsView.render().$el.appendTo(document.body);
      this.itemsView.on('click', function (uuid, model) {
        that.showComponentViewer(model.toJSON());
      });
    },

    save: function () {
      this.items.save();
    },

    load: function () {
      this.items.load();
    },

    addComponent: function (options) {
      if (options == null || typeof options.uuid !== 'string') {
        throw new Error('You must specify the component\'s uuid');
      }
      this.items.add(options);
      this.save();
    },

    openComponent: function (options) {
      if (options == null || typeof options.uuid !== 'string') {
        throw new Error('You must specify the component\'s uuid');
      }
      this.showComponentViewer(options);
    },

    showComponentViewer: function (options) {
      var that = this;
      if (this.viewerView != null) {
        this.viewerView.close();
      }
      this.viewerView = new ViewerView({
        model: new Item(options)
      });
      this.viewerView.on('minimize', function (model) {
        that.addComponent(model.toJSON());
      });
      this.viewerView.render().$el.appendTo(document.body);
    }
  };

  Workspace.getInstance = function () {
    if (instance == null) {
      instance = new Workspace();
    }
    return instance;
  };

  return Workspace.getInstance();

});
