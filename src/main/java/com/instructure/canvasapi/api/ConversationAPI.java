package com.instructure.canvasapi.api;

import android.content.Context;

import com.instructure.canvasapi.model.Conversation;
import com.instructure.canvasapi.utilities.APIHelpers;
import com.instructure.canvasapi.utilities.CanvasCallback;
import com.instructure.canvasapi.utilities.CanvasRestAdapter;

import java.util.ArrayList;

import retrofit.Callback;
import retrofit.RestAdapter;
import retrofit.client.Response;
import retrofit.http.DELETE;
import retrofit.http.EncodedQuery;
import retrofit.http.GET;
import retrofit.http.POST;
import retrofit.http.PUT;
import retrofit.http.Path;
import retrofit.http.Query;

/**
 * Created by Josh Ruesch on 8/9/13.
 *
 * Copyright (c) 2014 Instructure. All rights reserved.
 */
public class ConversationAPI {

    public enum ConversationScope { ALL,UNREAD,ARCHIVED,STARRED,SENT }
    private static String conversationScopeToString(ConversationScope scope){
        if(scope == ConversationScope.UNREAD) {
            return "unread";
        } else if (scope == ConversationScope.STARRED) {
            return "starred";
        } else if (scope == ConversationScope.ARCHIVED) {
            return "archived";
        } else if (scope == ConversationScope.SENT) {
            return "sent";
        }
        return "";
    }

    public enum WorkflowState {READ,UNREAD,ARCHIVED}

    private static String conversationStateToString(WorkflowState state){
        if(state == WorkflowState.UNREAD) {
            return "unread";
        } else if (state == WorkflowState.READ) {
            return "read";
        } else if (state == WorkflowState.ARCHIVED) {
            return "archived";
        }
        return "";
    }

    private static String getFirstPageConversationsCacheFilename(ConversationScope scope){
        return "/conversations/" + conversationScopeToString(scope);
    }

    private static String getDetailedConversationCacheFilename(long conversation_id){
        return "/conversations/" + conversation_id;
    }

    interface ConversationsInterface {
        @GET("/conversations/?interleave_submissions=1")
        void getFirstPageConversationList(@Query("scope") String scope, Callback<Conversation[]> callback);


        @GET("/{next}")
        void getNextPageConversationList(@Path(value = "next", encode = false) String nextURL, Callback<Conversation[]>callback);

        @GET("/conversations/{id}/?interleave_submissions=1")
        void getDetailedConversation(@Path("id") long conversation_id, @Query("auto_mark_as_read") int markAsRead, Callback<Conversation> callback);

        @POST("/conversations/{id}/add_message")
        void addMessageToConversation(@Path("id")long conversation_id, @Query("body")String message, CanvasCallback<Conversation> callback);

        @POST("/conversations?mode=sync")
        void createConversation(@EncodedQuery("recipients[]") String recipients, @Query("body") String message, @Query("group_conversation") int group, CanvasCallback<Response> callback);

        @DELETE("/conversations/{conversationid}")
        void deleteConversation(@Path("conversationid")long conversationID, CanvasCallback<Response>responseCallback);

        @PUT("/conversations/{conversationid}?conversation[workflow_state]=unread")
        void markConversationAsUnread(@Path("conversationid")long conversationID, CanvasCallback<Response>responseCallback);

        @PUT("/conversations/{conversationid}?conversation[workflow_state]=archived")
        void archiveConversation(@Path("conversationid")long conversationID, CanvasCallback<Response>responseCallback);

        @PUT("/conversations/{conversationid}?conversation[workflow_state]=read")
        void unArchiveConversation(@Path("conversationid")long conversationID, CanvasCallback<Response>responseCallback);

        @PUT("/conversations/{conversationid}")
        void setIsStarred(@Path("conversationid")long conversationID, @Query("conversation[starred]") boolean isStarred, CanvasCallback<Conversation>responseCallback);

        @PUT("/conversations/{conversationid}")
        void setIsSubscribed(@Path("conversationid")long conversationID, @Query("conversation[subscribed]") boolean isSubscribed, CanvasCallback<Conversation>responseCallback);

        @PUT("/conversations/{conversationid}")
        void setSubject(@Path("conversationid")long conversationID, @Query("conversation[subject]") String subject, CanvasCallback<Conversation>responseCallback);

        @PUT("/conversations/{conversationid}")
        void setWorkflowState(@Path("conversationid")long conversationID, @Query("conversation[workflow_state]") String workflowState, CanvasCallback<Conversation>responseCallback);

        /////////////////////////////////////////////////////////////////////////////
        // Synchronous
        /////////////////////////////////////////////////////////////////////////////

        @GET("/conversations/?interleave_submissions=1")
        Conversation[] getFirstPageConversationList(@Query("scope") String scope, @Query("per_page") int number);

        @GET("/conversations/{id}/?interleave_submissions=1")
        Conversation getDetailedConversationSynchronous(@Path("id") long conversation_id);

    }

    /////////////////////////////////////////////////////////////////////////
    // Build Interface Helpers
    /////////////////////////////////////////////////////////////////////////

    private static ConversationsInterface buildInterface(CanvasCallback<?> callback) {
        return buildInterface(callback.getContext());
    }

    private static ConversationsInterface buildInterface(Context context) {
        RestAdapter restAdapter = CanvasRestAdapter.buildAdapter(context);
        return restAdapter.create(ConversationsInterface.class);
    }

    /////////////////////////////////////////////////////////////////////////
    // API Calls
    /////////////////////////////////////////////////////////////////////////

    public static void getDetailedConversation(CanvasCallback<Conversation> callback, long conversation_id, boolean markAsRead) {
        if (APIHelpers.paramIsNull(callback)) return;

        callback.readFromCache(getDetailedConversationCacheFilename(conversation_id));
        buildInterface(callback).getDetailedConversation(conversation_id, APIHelpers.booleanToInt(markAsRead), callback);
    }

    public static void getFirstPageConversations(CanvasCallback<Conversation[]> callback, ConversationScope scope) {
        if (APIHelpers.paramIsNull(callback)) return;

        callback.readFromCache(getFirstPageConversationsCacheFilename(scope));
        buildInterface(callback).getFirstPageConversationList(conversationScopeToString(scope), callback);
    }


    public static void getNextPageConversations(CanvasCallback<Conversation[]> callback, String nextURL){
        if (APIHelpers.paramIsNull(callback, nextURL)) return;

        callback.setIsNextPage(true);
        buildInterface(callback).getNextPageConversationList(nextURL, callback);
    }

    public static void addMessageToConversation(CanvasCallback<Conversation> callback, long conversation_id, String body){
        if (APIHelpers.paramIsNull(callback, body)) return;

        buildInterface(callback).addMessageToConversation(conversation_id, body, callback);
    }

    public static void createConversation(CanvasCallback<Response> callback, ArrayList<String> userIDs, String message, boolean groupBoolean){
        if(APIHelpers.paramIsNull(callback,userIDs,message)){return;}

        //The message has to be sent to somebody.
        if(userIDs.size() == 0){return;}

        //Manually build the recipients string.
        String recipientKey = "recipients[]";
        String recipientsParameter = userIDs.get(0);
        for(int i = 1; i < userIDs.size();i++)
        {
            recipientsParameter += "&"+recipientKey+"="+userIDs.get(i);
        }

        //Get the boolean parameter.
        int group = APIHelpers.booleanToInt(groupBoolean);


        buildInterface(callback).createConversation(recipientsParameter, message, group, callback);
    }

    public static void deleteConversation(CanvasCallback<Response>responseCanvasCallback, long conversationId){
        if(APIHelpers.paramIsNull(responseCanvasCallback)){return;}

        buildInterface(responseCanvasCallback).deleteConversation(conversationId, responseCanvasCallback);
    }

    public static void markConversationAsUnread(CanvasCallback<Response>responseCanvasCallback, long conversationId){
        if(APIHelpers.paramIsNull(responseCanvasCallback)){return;}

        buildInterface(responseCanvasCallback).markConversationAsUnread(conversationId, responseCanvasCallback);
    }


    public static void archiveConversation(CanvasCallback<Response>responseCanvasCallback, long conversationId){
        if(APIHelpers.paramIsNull(responseCanvasCallback)){return;}

        buildInterface(responseCanvasCallback).archiveConversation(conversationId, responseCanvasCallback);
    }

    public static void unArchiveConversation(CanvasCallback<Response>responseCanvasCallback, long conversationId){
        if(APIHelpers.paramIsNull(responseCanvasCallback)){return;}

        buildInterface(responseCanvasCallback).unArchiveConversation(conversationId, responseCanvasCallback);
    }

    public static void subscribeToConversation(long conversationId, boolean isSubscribed, CanvasCallback<Conversation>responseCanvasCallback){
        if(APIHelpers.paramIsNull(responseCanvasCallback)){return;}

        buildInterface(responseCanvasCallback).setIsSubscribed(conversationId, isSubscribed, responseCanvasCallback);
    }

    public static void starConversation(long conversationId, boolean isStarred, CanvasCallback<Conversation>responseCanvasCallback){
        if(APIHelpers.paramIsNull(responseCanvasCallback)){return;}

        buildInterface(responseCanvasCallback).setIsStarred(conversationId, isStarred, responseCanvasCallback);
    }

    public static void setConversationSubject(long conversationId, String newSubject, CanvasCallback<Conversation>responseCanvasCallback){
        if(APIHelpers.paramIsNull(responseCanvasCallback)){return;}

        buildInterface(responseCanvasCallback).setSubject(conversationId, newSubject, responseCanvasCallback);
    }

    public static void setConversationWorkflowState(long conversationId, WorkflowState workflowState, CanvasCallback<Conversation>responseCanvasCallback){
        if(APIHelpers.paramIsNull(responseCanvasCallback)){return;}

        buildInterface(responseCanvasCallback).setWorkflowState(conversationId, conversationStateToString(workflowState), responseCanvasCallback);
    }

    /////////////////////////////////////////////////////////////////////////////
    // Synchronous
    //
    // If Retrofit is unable to parse (no network for example) Synchronous calls
    // will throw a nullPointer exception. All synchronous calls need to be in a
    // try catch block.
    /////////////////////////////////////////////////////////////////////////////

    public static Conversation[] getFirstPageConversationsSynchronous(ConversationScope scope, Context context, int numberToReturn) {

        try{
            return buildInterface(context).getFirstPageConversationList(conversationScopeToString(scope), numberToReturn);
        } catch (Exception E){
            return null;
        }
    }

    public static Conversation getDetailedConversationSynchronous(Context context, long conversation_id){
        try {
            return buildInterface(context).getDetailedConversationSynchronous(conversation_id);
        } catch (Exception E){
            return null;
        }
    }



}
