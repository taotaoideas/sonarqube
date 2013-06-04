/*
 * SonarQube, open source software quality management tool.
 * Copyright (C) 2008-2013 SonarSource
 * mailto:contact AT sonarsource DOT com
 *
 * SonarQube is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * SonarQube is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */

package org.sonar.server.issue;

import com.google.common.base.Function;
import com.google.common.collect.Iterables;
import org.sonar.api.ServerComponent;
import org.sonar.api.issue.ActionPlan;
import org.sonar.api.web.UserRole;
import org.sonar.core.issue.ActionPlanDeadlineComparator;
import org.sonar.core.issue.ActionPlanStats;
import org.sonar.core.issue.db.ActionPlanDao;
import org.sonar.core.issue.db.ActionPlanDto;
import org.sonar.core.issue.db.ActionPlanStatsDao;
import org.sonar.core.issue.db.ActionPlanStatsDto;
import org.sonar.core.resource.ResourceDao;
import org.sonar.core.resource.ResourceDto;
import org.sonar.core.resource.ResourceQuery;
import org.sonar.core.user.AuthorizationDao;
import org.sonar.server.user.UserSession;

import javax.annotation.CheckForNull;

import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import static com.google.common.collect.Lists.newArrayList;

/**
 * @since 3.6
 */
public class ActionPlanService implements ServerComponent {

  private final ActionPlanDao actionPlanDao;
  private final ActionPlanStatsDao actionPlanStatsDao;
  private final ResourceDao resourceDao;
  private final AuthorizationDao authorizationDao;

  public ActionPlanService(ActionPlanDao actionPlanDao, ActionPlanStatsDao actionPlanStatsDao, ResourceDao resourceDao, AuthorizationDao authorizationDao) {
    this.actionPlanDao = actionPlanDao;
    this.actionPlanStatsDao = actionPlanStatsDao;
    this.resourceDao = resourceDao;
    this.authorizationDao = authorizationDao;
  }

  public ActionPlan create(ActionPlan actionPlan, UserSession userSession) {
    ResourceDto project = findProject(actionPlan.projectKey());
    checkAuthorization(userSession, project, UserRole.ADMIN);
    actionPlanDao.save(ActionPlanDto.toActionDto(actionPlan, project.getId()));
    return actionPlan;
  }

  public ActionPlan update(ActionPlan actionPlan, UserSession userSession) {
    ResourceDto project = findProject(actionPlan.projectKey());
    checkAuthorization(userSession, project, UserRole.ADMIN);
    actionPlanDao.update(ActionPlanDto.toActionDto(actionPlan, project.getId()));
    return actionPlan;
  }

  public void delete(String actionPlanKey, UserSession userSession) {
    checkAuthorization(userSession, findActionPlanDto(actionPlanKey).getProjectKey(), UserRole.ADMIN);
    actionPlanDao.delete(actionPlanKey);
  }

  public ActionPlan setStatus(String actionPlanKey, String status, UserSession userSession) {
    ActionPlanDto actionPlanDto = findActionPlanDto(actionPlanKey);
    checkAuthorization(userSession, actionPlanDto.getProjectKey(), UserRole.ADMIN);

    actionPlanDto.setStatus(status);
    actionPlanDto.setCreatedAt(new Date());
    actionPlanDao.update(actionPlanDto);
    return actionPlanDto.toActionPlan();
  }

  @CheckForNull
  public ActionPlan findByKey(String key, UserSession userSession) {
    ActionPlanDto actionPlanDto = actionPlanDao.findByKey(key);
    if (actionPlanDto == null) {
      return null;
    }
    checkAuthorization(userSession, actionPlanDto.getProjectKey(), UserRole.USER);
    return actionPlanDto.toActionPlan();
  }

  public List<ActionPlan> findByKeys(Collection<String> keys) {
    List<ActionPlanDto> actionPlanDtos = actionPlanDao.findByKeys(keys);
    return toActionPlans(actionPlanDtos);
  }

  public Collection<ActionPlan> findOpenByProjectKey(String projectKey, UserSession userSession) {
    ResourceDto project = findProject(projectKey);
    checkAuthorization(userSession, project, UserRole.USER);

    List<ActionPlanDto> dtos = actionPlanDao.findOpenByProjectId(project.getId());
    List<ActionPlan> plans = toActionPlans(dtos);
    Collections.sort(plans, new ActionPlanDeadlineComparator());
    return plans;
  }

  public List<ActionPlanStats> findActionPlanStats(String projectKey, UserSession userSession) {
    ResourceDto project = findProject(projectKey);
    checkAuthorization(userSession, project, UserRole.USER);

    List<ActionPlanStatsDto> actionPlanStatsDtos = actionPlanStatsDao.findByProjectId(project.getId());
    List<ActionPlanStats> actionPlanStats = newArrayList(Iterables.transform(actionPlanStatsDtos, new Function<ActionPlanStatsDto, ActionPlanStats>() {
      @Override
      public ActionPlanStats apply(ActionPlanStatsDto actionPlanStatsDto) {
        return actionPlanStatsDto.toActionPlanStat();
      }
    }));
    Collections.sort(actionPlanStats, new ActionPlanDeadlineComparator());
    return actionPlanStats;
  }

  public boolean isNameAlreadyUsedForProject(String name, String projectKey) {
    return !actionPlanDao.findByNameAndProjectId(name, findProject(projectKey).getId()).isEmpty();
  }

  private List<ActionPlan> toActionPlans(List<ActionPlanDto> actionPlanDtos) {
    return newArrayList(Iterables.transform(actionPlanDtos, new Function<ActionPlanDto, ActionPlan>() {
      @Override
      public ActionPlan apply(ActionPlanDto actionPlanDto) {
        return actionPlanDto.toActionPlan();
      }
    }));
  }

  private ActionPlanDto findActionPlanDto(String actionPlanKey) {
    ActionPlanDto actionPlanDto = actionPlanDao.findByKey(actionPlanKey);
    if (actionPlanDto == null) {
      throw new IllegalArgumentException("Action plan " + actionPlanKey + " has not been found.");
    }
    return actionPlanDto;
  }

  private ResourceDto findProject(String projectKey) {
    ResourceDto resourceDto = resourceDao.getResource(ResourceQuery.create().setKey(projectKey));
    if (resourceDto == null) {
      throw new IllegalArgumentException("Project " + projectKey + " does not exists.");
    }
    return resourceDto;
  }

  private void checkAuthorization(UserSession userSession, String projectKey, String requiredRole) {
    checkAuthorization(userSession, findProject(projectKey), requiredRole);
  }

  private void checkAuthorization(UserSession userSession, ResourceDto project, String requiredRole) {
    if (!userSession.isLoggedIn()) {
      // must be logged
      throw new IllegalStateException("User is not logged in");
    }
    if (!authorizationDao.isAuthorizedComponentId(project.getId(), userSession.userId(), requiredRole)) {
      throw new IllegalStateException("User does not have the required role to access the project: " + project.getKey());
    }
  }

}
