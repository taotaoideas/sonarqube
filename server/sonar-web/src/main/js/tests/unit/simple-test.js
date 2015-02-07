define([
      'coding-rules/models/rule',
      'coding-rules/rule/custom-rule-view'
    ],
    function (Rule, CustomRuleView) {

      describe('just checking', function () {

        it('works', function () {
          var rule = new Rule({ key: 'ID' });
          expect(rule.id).toBe('ID');
        });

        it('works again', function () {
          var rule = new Rule();
          rule.addExtraAttributes([]);
        });

        it('works with views', function () {
          var view = new CustomRuleView();
          expect(view).toBeDefined();
        });

      });

    });
