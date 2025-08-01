/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.kie.kogito.index.jpa.storage;

import java.net.URI;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import org.kie.kogito.event.usertask.MultipleUserTaskInstanceDataEvent;
import org.kie.kogito.event.usertask.UserTaskInstanceAssignmentDataEvent;
import org.kie.kogito.event.usertask.UserTaskInstanceAssignmentEventBody;
import org.kie.kogito.event.usertask.UserTaskInstanceAttachmentDataEvent;
import org.kie.kogito.event.usertask.UserTaskInstanceAttachmentEventBody;
import org.kie.kogito.event.usertask.UserTaskInstanceCommentDataEvent;
import org.kie.kogito.event.usertask.UserTaskInstanceCommentEventBody;
import org.kie.kogito.event.usertask.UserTaskInstanceDataEvent;
import org.kie.kogito.event.usertask.UserTaskInstanceDeadlineDataEvent;
import org.kie.kogito.event.usertask.UserTaskInstanceStateDataEvent;
import org.kie.kogito.event.usertask.UserTaskInstanceStateEventBody;
import org.kie.kogito.event.usertask.UserTaskInstanceVariableDataEvent;
import org.kie.kogito.event.usertask.UserTaskInstanceVariableEventBody;
import org.kie.kogito.index.CommonUtils;
import org.kie.kogito.index.DateTimeUtils;
import org.kie.kogito.index.jpa.mapper.UserTaskInstanceEntityMapper;
import org.kie.kogito.index.jpa.model.AttachmentEntity;
import org.kie.kogito.index.jpa.model.CommentEntity;
import org.kie.kogito.index.jpa.model.UserTaskInstanceEntity;
import org.kie.kogito.index.model.UserTaskInstance;
import org.kie.kogito.index.storage.UserTaskInstanceStorage;
import org.kie.kogito.jackson.utils.JsonObjectUtils;
import org.kie.kogito.jackson.utils.ObjectMapperFactory;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.net.UrlEscapers;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;

import static java.lang.String.format;
import static org.kie.kogito.index.DateTimeUtils.toZonedDateTime;

@ApplicationScoped
public class UserTaskInstanceEntityStorage extends AbstractJPAStorageFetcher<String, UserTaskInstanceEntity, UserTaskInstance> implements UserTaskInstanceStorage {

    protected UserTaskInstanceEntityStorage() {
    }

    @Inject
    public UserTaskInstanceEntityStorage(EntityManager em) {
        super(em, UserTaskInstanceEntity.class, UserTaskInstanceEntityMapper.INSTANCE::mapToModel);
    }

    @Override
    @Transactional
    public void indexGroup(MultipleUserTaskInstanceDataEvent events) {
        Map<String, UserTaskInstanceEntity> taskMap = new HashMap<>();
        for (UserTaskInstanceDataEvent<?> event : events.getData()) {
            indexEvent(taskMap.computeIfAbsent(event.getKogitoUserTaskInstanceId(), id -> findOrInit(id)), event);
        }
    }

    @Override
    @Transactional
    public void indexAssignment(UserTaskInstanceAssignmentDataEvent event) {
        indexAssignment(findOrInit(event), event);
    }

    @Override
    @Transactional
    public void indexAttachment(UserTaskInstanceAttachmentDataEvent event) {
        indexAttachment(findOrInit(event), event);
    }

    @Override
    @Transactional
    public void indexDeadline(UserTaskInstanceDeadlineDataEvent event) {
        indexDeadline(findOrInit(event), event);
    }

    @Override
    @Transactional
    public void indexState(UserTaskInstanceStateDataEvent event) {
        indexState(findOrInit(event), event);
    }

    @Override
    @Transactional
    public void indexComment(UserTaskInstanceCommentDataEvent event) {
        indexComment(findOrInit(event), event);
    }

    @Override
    @Transactional
    public void indexVariable(UserTaskInstanceVariableDataEvent event) {
        indexVariable(findOrInit(event), event);
    }

    private void indexEvent(UserTaskInstanceEntity task, UserTaskInstanceDataEvent<?> event) {
        if (event instanceof UserTaskInstanceAssignmentDataEvent) {
            indexAssignment(task, (UserTaskInstanceAssignmentDataEvent) event);
        } else if (event instanceof UserTaskInstanceAttachmentDataEvent) {
            indexAttachment(task, (UserTaskInstanceAttachmentDataEvent) event);
        } else if (event instanceof UserTaskInstanceDeadlineDataEvent) {
            indexDeadline(task, (UserTaskInstanceDeadlineDataEvent) event);
        } else if (event instanceof UserTaskInstanceStateDataEvent) {
            indexState(task, (UserTaskInstanceStateDataEvent) event);
        } else if (event instanceof UserTaskInstanceCommentDataEvent) {
            indexComment(task, (UserTaskInstanceCommentDataEvent) event);
        } else if (event instanceof UserTaskInstanceVariableDataEvent) {
            indexVariable(task, (UserTaskInstanceVariableDataEvent) event);
        }
    }

    private void indexAssignment(UserTaskInstanceEntity userTaskInstance, UserTaskInstanceAssignmentDataEvent event) {
        UserTaskInstanceAssignmentEventBody body = event.getData();
        switch (body.getAssignmentType()) {
            case "USER_OWNERS":
                userTaskInstance.setPotentialUsers(new HashSet<>(body.getUsers()));
                break;
            case "USER_GROUPS":
                userTaskInstance.setPotentialGroups(new HashSet<>(body.getUsers()));
                break;
            case "USERS_EXCLUDED":
                userTaskInstance.setExcludedUsers(new HashSet<>(body.getUsers()));
                break;
            case "ADMIN_GROUPS":
                userTaskInstance.setAdminGroups(new HashSet<>(body.getUsers()));
                break;
            case "ADMIN_USERS":
                userTaskInstance.setAdminUsers(new HashSet<>(body.getUsers()));
                break;
        }
    }

    private void indexAttachment(UserTaskInstanceEntity userTaskInstance, UserTaskInstanceAttachmentDataEvent event) {
        UserTaskInstanceAttachmentEventBody body = event.getData();
        List<AttachmentEntity> attachments = userTaskInstance.getAttachments();
        switch (body.getEventType()) {
            case UserTaskInstanceAttachmentEventBody.EVENT_TYPE_ADDED:
            case UserTaskInstanceAttachmentEventBody.EVENT_TYPE_CHANGE:
                AttachmentEntity attachment = attachments.stream().filter(e -> e.getId().equals(body.getAttachmentId())).findAny().orElseGet(() -> {
                    AttachmentEntity newAttachment = new AttachmentEntity();
                    newAttachment.setUserTask(userTaskInstance);
                    attachments.add(newAttachment);
                    return newAttachment;
                });
                attachment.setId(body.getAttachmentId());
                attachment.setName(body.getAttachmentName());
                attachment.setContent(body.getAttachmentURI().toString());
                attachment.setUpdatedBy(body.getEventUser() != null ? body.getEventUser() : "unknown");
                attachment.setUpdatedAt(DateTimeUtils.toZonedDateTime(body.getEventDate()));
                break;
            case UserTaskInstanceAttachmentEventBody.EVENT_TYPE_DELETED:
                attachments.removeIf(e -> e.getId().equals(body.getAttachmentId()));
                break;
        }
    }

    private void indexDeadline(UserTaskInstanceEntity userTaskInstance, UserTaskInstanceDeadlineDataEvent event) {
        // deadlines ignored for now
    }

    private void indexState(UserTaskInstanceEntity task, UserTaskInstanceStateDataEvent event) {
        UserTaskInstanceStateEventBody body = event.getData();
        task.setProcessInstanceId(body.getProcessInstanceId());
        task.setProcessId(event.getKogitoProcessId());
        task.setRootProcessId(event.getKogitoRootProcessId());
        task.setRootProcessInstanceId(event.getKogitoRootProcessInstanceId());
        task.setName(body.getUserTaskName());
        task.setDescription(body.getUserTaskDescription());
        task.setState(body.getState());
        task.setPriority(body.getUserTaskPriority());
        if (task.getStarted() == null) {
            task.setStarted(toZonedDateTime(event.getData().getEventDate()));
        } else if (CommonUtils.isTaskCompleted(event.getData().getEventType())) {
            task.setCompleted(toZonedDateTime(event.getData().getEventDate()));
        }
        task.setActualOwner(event.getData().getActualOwner());
        task.setEndpoint(
                event.getSource() == null ? null : getEndpoint(event.getSource(), event.getData().getProcessInstanceId(), event.getData().getUserTaskName(), event.getData().getExternalReferenceId()));
        task.setLastUpdate(toZonedDateTime(event.getData().getEventDate()));
        task.setReferenceName(event.getData().getUserTaskReferenceName());
        task.setExternalReferenceId(body.getExternalReferenceId());
        task.setSlaDueDate(toZonedDateTime(body.getSlaDueDate()));
    }

    private String getEndpoint(URI source, String pId, String taskName, String taskId) {
        String name = UrlEscapers.urlPathSegmentEscaper().escape(taskName);
        return source.toString() + format("/%s/%s/%s", pId, name, taskId);
    }

    private void indexComment(UserTaskInstanceEntity userTaskInstance, UserTaskInstanceCommentDataEvent event) {
        UserTaskInstanceCommentEventBody body = event.getData();
        List<CommentEntity> comments = userTaskInstance.getComments();
        switch (body.getEventType()) {
            case UserTaskInstanceCommentEventBody.EVENT_TYPE_ADDED:
            case UserTaskInstanceCommentEventBody.EVENT_TYPE_CHANGE:
                CommentEntity comment = comments.stream().filter(e -> e.getId().equals(body.getCommentId())).findAny().orElseGet(() -> {
                    CommentEntity newComment = new CommentEntity();
                    newComment.setUserTask(userTaskInstance);
                    comments.add(newComment);
                    return newComment;
                });
                comment.setId(body.getCommentId());
                comment.setContent(body.getCommentContent());
                comment.setUpdatedBy(body.getEventUser() != null ? body.getEventUser() : "unknown");
                comment.setUpdatedAt(DateTimeUtils.toZonedDateTime(body.getEventDate()));

                break;
            case UserTaskInstanceCommentEventBody.EVENT_TYPE_DELETED:
                comments.removeIf(e -> e.getId().equals(body.getCommentId()));
                break;
        }
    }

    private void indexVariable(UserTaskInstanceEntity userTaskInstance, UserTaskInstanceVariableDataEvent event) {
        UserTaskInstanceVariableEventBody body = event.getData();
        if (body.getVariableType().equals("INPUT")) {
            ObjectNode objectNode = userTaskInstance.getInputs();
            if (objectNode == null) {
                objectNode = ObjectMapperFactory.get().createObjectNode();
            }
            objectNode.set(body.getVariableName(), JsonObjectUtils.fromValue(body.getVariableValue()));
            userTaskInstance.setInputs(objectNode);
        } else {
            ObjectNode objectNode = userTaskInstance.getOutputs();
            if (objectNode == null) {
                objectNode = ObjectMapperFactory.get().createObjectNode();
            }
            objectNode.set(body.getVariableName(), JsonObjectUtils.fromValue(body.getVariableValue()));
            userTaskInstance.setOutputs(objectNode);
        }
    }

    private UserTaskInstanceEntity findOrInit(UserTaskInstanceDataEvent<?> event) {
        return findOrInit(event.getKogitoUserTaskInstanceId());
    }

    private UserTaskInstanceEntity findOrInit(String taskId) {
        UserTaskInstanceEntity ut = em.find(UserTaskInstanceEntity.class, taskId);
        if (ut == null) {
            ut = new UserTaskInstanceEntity();
            ut.setId(taskId);
            em.persist(ut);
        }
        return ut;

    }
}
