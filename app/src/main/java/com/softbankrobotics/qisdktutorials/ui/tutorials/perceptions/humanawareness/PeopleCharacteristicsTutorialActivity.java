/*
 * Copyright (C) 2018 Softbank Robotics Europe
 * See COPYING for the license
 */

package com.softbankrobotics.qisdktutorials.ui.tutorials.perceptions.humanawareness;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.support.v7.widget.DividerItemDecoration;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.widget.Button;

import com.aldebaran.qi.Future;
import com.aldebaran.qi.sdk.QiContext;
import com.aldebaran.qi.sdk.QiSDK;
import com.aldebaran.qi.sdk.RobotLifecycleCallbacks;
import com.aldebaran.qi.sdk.builder.SayBuilder;
import com.aldebaran.qi.sdk.object.actuation.Actuation;
import com.aldebaran.qi.sdk.object.actuation.Frame;
import com.aldebaran.qi.sdk.object.conversation.ConversationStatus;
import com.aldebaran.qi.sdk.object.conversation.Say;
import com.aldebaran.qi.sdk.object.geometry.Transform;
import com.aldebaran.qi.sdk.object.geometry.TransformTime;
import com.aldebaran.qi.sdk.object.geometry.Vector3;
import com.aldebaran.qi.sdk.object.human.AttentionState;
import com.aldebaran.qi.sdk.object.human.EngagementIntentionState;
import com.aldebaran.qi.sdk.object.human.ExcitementState;
import com.aldebaran.qi.sdk.object.human.Gender;
import com.aldebaran.qi.sdk.object.human.Human;
import com.aldebaran.qi.sdk.object.human.PleasureState;
import com.aldebaran.qi.sdk.object.human.SmileState;
import com.aldebaran.qi.sdk.object.humanawareness.HumanAwareness;
import com.softbankrobotics.qisdktutorials.R;
import com.softbankrobotics.qisdktutorials.ui.conversation.ConversationBinder;
import com.softbankrobotics.qisdktutorials.ui.conversation.ConversationView;
import com.softbankrobotics.qisdktutorials.ui.tutorials.TutorialActivity;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

/**
 * The activity for the People characteristics tutorial.
 */
public class PeopleCharacteristicsTutorialActivity extends TutorialActivity implements RobotLifecycleCallbacks {

    private static final String TAG = "CharacteristicsActivity";

    private ConversationView conversationView;
    private ConversationBinder conversationBinder;

    private HumanInfoAdapter humanInfoAdapter;

    // Store the HumanAwareness service.
    private HumanAwareness humanAwareness;
    // The QiContext provided by the QiSDK.
    private QiContext qiContext;

    private List<HumanInfo> humanInfoList = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        conversationView = findViewById(R.id.conversationView);

        RecyclerView recyclerView = findViewById(R.id.recyclerview);
        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        recyclerView.setLayoutManager(layoutManager);
        recyclerView.addItemDecoration(new DividerItemDecoration(this, layoutManager.getOrientation()));
        humanInfoAdapter = new HumanInfoAdapter();
        recyclerView.setAdapter(humanInfoAdapter);

        // Find humans around when refresh button clicked.
        Button refreshButton = findViewById(R.id.refresh_button);
        refreshButton.setOnClickListener(v -> {
            if (qiContext != null) {
                findHumansAround();
            }
        });

        // Register the RobotLifecycleCallbacks to this Activity.
        QiSDK.register(this, this);
    }

    @Override
    protected void onDestroy() {
        // Unregister the RobotLifecycleCallbacks for this Activity.
        QiSDK.unregister(this, this);
        super.onDestroy();
    }

    @Override
    protected int getLayoutId() {
        return R.layout.activity_people_characteristics_tutorial;
    }

    @Override
    public void onRobotFocusGained(final QiContext qiContext) {
        // Store the provided QiContext.
        this.qiContext = qiContext;

        // Bind the conversational events to the view.
        ConversationStatus conversationStatus = qiContext.getConversation().status(qiContext.getRobotContext());
        conversationBinder = conversationView.bindConversationTo(conversationStatus);

        Say say = SayBuilder.with(qiContext)
                .withText("I can display characteristics about the human I'm seeing.")
                .build();

        say.run();

        // Get the HumanAwareness service from the QiContext.
        humanAwareness = qiContext.getHumanAwareness();

        findHumansAround();
    }

    @Override
    public void onRobotFocusLost() {
        // Remove the QiContext.
        this.qiContext = null;

        if (conversationBinder != null) {
            conversationBinder.unbind();
        }
    }

    @Override
    public void onRobotFocusRefused(String reason) {
        // Nothing here.
    }

    private void findHumansAround() {
        if (humanAwareness != null) {
            // Get the humans around the robot.
            Future<List<Human>> humansAroundFuture = humanAwareness.async().getHumansAround();

            humansAroundFuture.andThenConsume(humansAround -> {
                Log.i(TAG, humansAround.size() + " human(s) around.");
                retrieveCharacteristics(humansAround);
            });
        }
    }

    private void retrieveCharacteristics(final List<Human> humans) {
        // Get the Actuation service from the QiContext.
        Actuation actuation = qiContext.getActuation();

        // Get the robot frame.
        Frame robotFrame = actuation.robotFrame();
        //we clear memory used for human who are being showed
        for (HumanInfo h : humanInfoList) {
            h.clearMemory();
        }

        humanInfoList = new ArrayList<>();
        for (int i = 0; i < humans.size(); i++) {
            // Get the human.
            Human human = humans.get(i);

            // Get the characteristics.
            Integer age = human.getEstimatedAge().getYears();
            Gender gender = human.getEstimatedGender();
            PleasureState pleasureState = human.getEmotion().getPleasure();
            ExcitementState excitementState = human.getEmotion().getExcitement();
            EngagementIntentionState engagementIntentionState = human.getEngagementIntention();
            SmileState smileState = human.getFacialExpressions().getSmile();
            AttentionState attentionState = human.getAttention();
            Frame humanFrame = human.getHeadFrame();

            // Display the characteristics.
            Log.i(TAG, "----- Human " + i + " -----");
            Log.i(TAG, "Age: " + age + " year(s)");
            Log.i(TAG, "Gender: " + gender);
            Log.i(TAG, "Pleasure state: " + pleasureState);
            Log.i(TAG, "Excitement state: " + excitementState);
            Log.i(TAG, "Engagement state: " + engagementIntentionState);
            Log.i(TAG, "Smile state: " + smileState);
            Log.i(TAG, "Attention state: " + attentionState);

            // Compute the distance.
            double distance = computeDistance(humanFrame, robotFrame);
            // Display the distance between the human and the robot.
            Log.i(TAG, "Distance: " + distance + " meter(s).");

            // Get face picture.
            ByteBuffer facePictureBuffer = human.getFacePicture().getImage().getData();
            facePictureBuffer.rewind();
            int pictureBufferSize = facePictureBuffer.remaining();
            byte[] facePictureArray = new byte[pictureBufferSize];
            facePictureBuffer.get(facePictureArray);

            Bitmap facePicture = null;
            // Test if the robot has an empty picture (this can happen when he detects a human but not the face).
            if (pictureBufferSize != 0) {
                Log.i(TAG, "Picture available");
                facePicture = BitmapFactory.decodeByteArray(facePictureArray, 0, pictureBufferSize);
            } else {
                Log.i(TAG, "Picture not available");
            }

            HumanInfo humanInfo = new HumanInfo(age, gender, pleasureState, excitementState, engagementIntentionState, smileState, attentionState, distance, facePicture);
            humanInfoList.add(humanInfo);
        }

        displayHumanInfoList(humanInfoList);
    }

    private double computeDistance(Frame humanFrame, Frame robotFrame) {
        // Get the TransformTime between the human frame and the robot frame.
        TransformTime transformTime = humanFrame.computeTransform(robotFrame);

        // Get the transform.
        Transform transform = transformTime.getTransform();

        // Get the translation.
        Vector3 translation = transform.getTranslation();

        // Get the x and y components of the translation.
        double x = translation.getX();
        double y = translation.getY();

        // Compute the distance and return it.
        return Math.sqrt(x * x + y * y);
    }

    private void displayHumanInfoList(final List<HumanInfo> humanInfoList) {
        runOnUiThread(() -> humanInfoAdapter.updateList(humanInfoList));
    }
}
