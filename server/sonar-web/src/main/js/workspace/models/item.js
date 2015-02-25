define(function () {

  return Backbone.Model.extend({
    idAttribute: 'uuid',

    validate: function () {
      if (!this.has('uuid')) {
        return 'uuid is missing';
      }
    },

    destroy: function (options) {
      this.stopListening();
      this.trigger('destroy', this, this.collection, options);
    }
  });

});
