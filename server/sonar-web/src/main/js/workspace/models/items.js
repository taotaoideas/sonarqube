define(['workspace/models/item'], function (Item) {

  var STORAGE_KEY = 'sonarqube-workspace';

  return Backbone.Collection.extend({
    model: Item,

    initialize: function () {
      this.on('remove', this.save);
    },

    save: function () {
      var dump = JSON.stringify(this.toJSON());
      window.localStorage.setItem(STORAGE_KEY, dump);
    },

    load: function () {
      var dump = window.localStorage.getItem(STORAGE_KEY);
      if (dump != null) {
        try {
          var parsed = JSON.parse(dump);
          this.reset(parsed);
        } catch (err) { }
      }
    }
  });

});
