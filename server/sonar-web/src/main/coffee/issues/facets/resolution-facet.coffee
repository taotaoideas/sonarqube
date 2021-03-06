#
# SonarQube, open source software quality management tool.
# Copyright (C) 2008-2014 SonarSource
# mailto:contact AT sonarsource DOT com
#
# SonarQube is free software; you can redistribute it and/or
# modify it under the terms of the GNU Lesser General Public
# License as published by the Free Software Foundation; either
# version 3 of the License, or (at your option) any later version.
#
# SonarQube is distributed in the hope that it will be useful,
# but WITHOUT ANY WARRANTY; without even the implied warranty of
# MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
# Lesser General Public License for more details.
#
# You should have received a copy of the GNU Lesser General Public License
# along with this program; if not, write to the Free Software Foundation,
# Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
#

define [
  'issues/facets/base-facet'
  'templates/issues'
], (
  BaseFacet
) ->

  $ = jQuery


  class extends BaseFacet
    template: Templates['issues-resolution-facet']


    onRender: ->
      super

      value = @options.app.state.get('query')['resolved']
      if value? && (!value || value == 'false')
        @$('.js-facet').filter("[data-unresolved]").addClass 'active'


    toggleFacet: (e) ->
      unresolved = $(e.currentTarget).is "[data-unresolved]"
      $(e.currentTarget).toggleClass 'active'
      if unresolved
        checked = $(e.currentTarget).is '.active'
        value = if checked then 'false' else null
        @options.app.state.updateFilter resolved: value, resolutions: null
      else
        @options.app.state.updateFilter resolved: null, resolutions: @getValue()


    disable: ->
      @options.app.state.updateFilter resolved: null, resolutions: null


    sortValues: (values) ->
      order = ['', 'FIXED', 'FALSE-POSITIVE', 'WONTFIX', 'REMOVED']
      _.sortBy values, (v) -> order.indexOf v.val

