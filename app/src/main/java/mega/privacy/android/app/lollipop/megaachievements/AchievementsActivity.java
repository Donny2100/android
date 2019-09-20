package mega.privacy.android.app.lollipop.megaachievements;

import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.ActionBar;
import android.support.v7.widget.Toolbar;
import android.util.DisplayMetrics;
import android.view.Display;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.FrameLayout;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;

import mega.privacy.android.app.MegaApplication;
import mega.privacy.android.app.R;
import mega.privacy.android.app.lollipop.AddContactActivityLollipop;
import mega.privacy.android.app.lollipop.LoginActivityLollipop;
import mega.privacy.android.app.lollipop.PinActivityLollipop;
import mega.privacy.android.app.lollipop.controllers.ContactController;
import nz.mega.sdk.MegaAchievementsDetails;
import nz.mega.sdk.MegaApiAndroid;
import nz.mega.sdk.MegaApiJava;
import nz.mega.sdk.MegaChatApi;
import nz.mega.sdk.MegaChatApiAndroid;
import nz.mega.sdk.MegaContactRequest;
import nz.mega.sdk.MegaError;
import nz.mega.sdk.MegaRequest;
import nz.mega.sdk.MegaRequestListenerInterface;

import static mega.privacy.android.app.utils.Constants.*;
import static mega.privacy.android.app.utils.LogUtil.*;
import static mega.privacy.android.app.utils.Util.*;

public class AchievementsActivity extends PinActivityLollipop implements MegaRequestListenerInterface {

    FrameLayout fragmentContainer;
    Toolbar tB;
    ActionBar aB;
    int visibleFragment;
    int achievementType;
    private AchievementsFragment achievementsFragment;
    private ReferralBonusesFragment referralBonusesFragment;
    private InviteFriendsFragment inviteFriendsFragment;
    private InfoAchievementsFragment infoAchievementsFragment;

    public MegaAchievementsDetails megaAchievements;
    public ArrayList<ReferralBonus> referralBonuses;

    MegaApiAndroid megaApi;
    MegaChatApiAndroid megaChatApi;

    private android.support.v7.app.AlertDialog successDialog;

    ArrayList<String> mails;
    ArrayList<String> pendingContacts;
    boolean pendingAttaches = false;

    DisplayMetrics outMetrics;

    protected void onCreate(Bundle savedInstanceState) {
        logDebug("onCreate");
		super.onCreate(savedInstanceState);

        if (megaApi == null){
            megaApi = ((MegaApplication) getApplication()).getMegaApi();
        }

        if(megaApi==null||megaApi.getRootNode()==null){
            logDebug("Refresh session - sdk");
            Intent intent = new Intent(this, LoginActivityLollipop.class);
            intent.putExtra("visibleFragment",  LOGIN_FRAGMENT);
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            startActivity(intent);
            finish();
            return;
        }
        if(isChatEnabled()){
            if (megaChatApi == null){
                megaChatApi = ((MegaApplication) getApplication()).getMegaChatApi();
            }

            if(megaChatApi==null||megaChatApi.getInitState()== MegaChatApi.INIT_ERROR){
                logDebug("Refresh session - karere");
                Intent intent = new Intent(this, LoginActivityLollipop.class);
                intent.putExtra("visibleFragment",  LOGIN_FRAGMENT);
                intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                startActivity(intent);
                finish();
                return;
            }
        }

        Display display = getWindowManager().getDefaultDisplay();
        outMetrics = new DisplayMetrics ();
        display.getMetrics(outMetrics);

        setContentView(R.layout.activity_achievements);

        fragmentContainer = (FrameLayout) findViewById(R.id.fragment_container_achievements);
        tB = (Toolbar) findViewById(R.id.toolbar_achievements);
        tB.setTitle(getString(R.string.achievements_title));
        tB.setVisibility(View.VISIBLE);
        setSupportActionBar(tB);
        aB = getSupportActionBar();
        logDebug("aB.setHomeAsUpIndicator_1");
        aB.setHomeAsUpIndicator(R.drawable.ic_arrow_back_white);
        aB.setHomeButtonEnabled(true);
        aB.setDisplayHomeAsUpEnabled(true);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            Window window = this.getWindow();
            window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
            window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
            window.setStatusBarColor(ContextCompat.getColor(this, R.color.lollipop_dark_primary_color));
        }

        visibleFragment = ACHIEVEMENTS_FRAGMENT;
        achievementsFragment = new AchievementsFragment();
        FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
        ft.replace(R.id.fragment_container_achievements, achievementsFragment, "achievementsFragment");
        ft.commitNow();

        referralBonuses = new ArrayList<>();
        megaApi.getAccountAchievements(this);

        if (savedInstanceState != null) {
            visibleFragment = savedInstanceState.getInt("visibleFragment", ACHIEVEMENTS_FRAGMENT);
            achievementType = savedInstanceState.getInt("achievementType", -1);
            mails = savedInstanceState.getStringArrayList("mails");
            pendingContacts = savedInstanceState.getStringArrayList("pendingContacts");
            pendingAttaches = savedInstanceState.getBoolean("pendingAttaches", false);
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        outState.putInt("visibleFragment", visibleFragment);
        outState.putInt("achievementType", achievementType);
        outState.putStringArrayList("mails", mails);
        outState.putStringArrayList("pendingContacts", pendingContacts);
        outState.putBoolean("pendingAttaches", pendingAttaches);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        logDebug("onOptionsItemSelected");

        int id = item.getItemId();
        switch(id) {
            case android.R.id.home: {
                if(visibleFragment==ACHIEVEMENTS_FRAGMENT){
                    finish();
                }
                else{
                    showFragment(ACHIEVEMENTS_FRAGMENT, -1);
                }

                break;
            }
        }
        return super.onOptionsItemSelected(item);
    }

    public void showFragment(int fragment, int type){
        logDebug("showFragment: " + fragment + " type: " + achievementType);
        visibleFragment = fragment;
        achievementType = type;

        if(visibleFragment==ACHIEVEMENTS_FRAGMENT){

            hideKeyboard(this, InputMethodManager.HIDE_NOT_ALWAYS);

            aB.setTitle(getString(R.string.achievements_title));
            if(achievementsFragment==null){
                achievementsFragment = new AchievementsFragment();
            }

            FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
            ft.replace(R.id.fragment_container_achievements, achievementsFragment, "achievementsFragment");
            ft.commitNow();

            achievementsFragment.updateValues();
        }
        else if(visibleFragment==INVITE_FRIENDS_FRAGMENT){

            aB.setTitle(getString(R.string.button_invite_friends));
            if(inviteFriendsFragment==null){
                inviteFriendsFragment = new InviteFriendsFragment();
            }

            FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
            ft.replace(R.id.fragment_container_achievements, inviteFriendsFragment, "inviteFriendsFragment");
            ft.commitNow();
        }
        else if(visibleFragment==BONUSES_FRAGMENT){

            if(referralBonusesFragment==null) {
                referralBonusesFragment = new ReferralBonusesFragment();
            }

            aB.setTitle(getString(R.string.title_referral_bonuses));
            FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
            ft.replace(R.id.fragment_container_achievements, referralBonusesFragment, "referralBonusesFragment");
            ft.commitNow();
        }
        else if(visibleFragment==INFO_ACHIEVEMENTS_FRAGMENT){
            Bundle bundle = new Bundle();
            bundle.putInt("achievementType", achievementType);

            infoAchievementsFragment = new InfoAchievementsFragment();
            infoAchievementsFragment.setArguments(bundle);

            FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
            ft.replace(R.id.fragment_container_achievements, infoAchievementsFragment, "infoAchievementsFragment");
            ft.commitNow();
        }
    }

    @Override
    public void onBackPressed() {
        logDebug("onBackPressedLollipop");
        retryConnectionsAndSignalPresence();

        if(visibleFragment==ACHIEVEMENTS_FRAGMENT){
            super.onBackPressed();
        }
        else{
            showFragment(ACHIEVEMENTS_FRAGMENT, -1);
        }
    }

    @Override
    public void onRequestStart(MegaApiJava api, MegaRequest request) {
        logDebug("onRequestStart: "+request.getRequestString());
    }

    @Override
    public void onRequestUpdate(MegaApiJava api, MegaRequest request) {

    }

    @Override
    public void onRequestFinish(MegaApiJava api, MegaRequest request, MegaError e) {
        logDebug("onRequestFinish: " + request.getRequestString() + "__" + e.getErrorCode());

        if(request.getType()==MegaRequest.TYPE_GET_ACHIEVEMENTS){
            if (e.getErrorCode() == MegaError.API_OK){

                megaAchievements=request.getMegaAchievementsDetails();
                if(megaAchievements!=null){
                    calculateReferralBonuses();
                    if(visibleFragment==ACHIEVEMENTS_FRAGMENT){
                        if(achievementsFragment.isAdded()){
                            achievementsFragment.updateValues();
                        }
                    }
                    else {
                        showFragment(visibleFragment, achievementType);
                    }
                    if (pendingAttaches){
                        attachPendig();
                    }
                }
            }
            else{
                showSnackbar(getString(R.string.cancel_subscription_error));
            }
        }
        else if (request.getType() == MegaRequest.TYPE_INVITE_CONTACT){
            logDebug("MegaRequest.TYPE_INVITE_CONTACT finished: " + request.getNumber());

            if (e.getErrorCode() == MegaError.API_OK){
                logDebug("OK INVITE CONTACT: " + request.getEmail());
                showInviteConfirmationDialog();
            }
            else{
                logWarning("Code: " + e.getErrorString());
                if(e.getErrorCode()==MegaError.API_EEXIST)
                {
                    showSnackbar(getString(R.string.context_contact_already_exists, request.getEmail()));
                }
                else if(request.getNumber()== MegaContactRequest.INVITE_ACTION_ADD && e.getErrorCode()==MegaError.API_EARGS)
                {
                    showSnackbar(getString(R.string.error_own_email_as_contact));
                }
                else{
                    showSnackbar(getString(R.string.general_error));
                }
                logError("ERROR: " + e.getErrorCode() + "___" + e.getErrorString());
            }
        }
    }

    public void inviteFriends(ArrayList<String> mails){
        logDebug("inviteFriends");
        hideKeyboard(this, InputMethodManager.HIDE_NOT_ALWAYS);

        showFragment(ACHIEVEMENTS_FRAGMENT, -1);

        ContactController cC = new ContactController(this);
        cC.inviteMultipleContacts(mails);
    }

    public void calculateReferralBonuses() {
        logDebug("calculateReferralBonuses");

        long count = megaAchievements.getAwardsCount();

        for (int i = 0; i < count; i++) {
            int type = megaAchievements.getAwardClass(i);

            int awardId = megaAchievements.getAwardId(i);

            int rewardId = megaAchievements.getRewardAwardId(awardId);
            logDebug("AWARD ID: " + awardId + " REWARD id: " + rewardId);

            if (type == MegaAchievementsDetails.MEGA_ACHIEVEMENT_INVITE) {

                ReferralBonus rBonus = new ReferralBonus();

                rBonus.setEmails(megaAchievements.getAwardEmails(i));

                long daysLeft = megaAchievements.getAwardExpirationTs(i);
                logDebug("Registration AwardExpirationTs: " + daysLeft);

                Calendar start = calculateDateFromTimestamp(daysLeft);
                Calendar end = Calendar.getInstance();
                Date startDate = start.getTime();
                Date endDate = end.getTime();
                long startTime = startDate.getTime();
                long endTime = endDate.getTime();
                long diffTime = startTime - endTime;
                long diffDays = diffTime / (1000 * 60 * 60 * 24);

                rBonus.setDaysLeft(diffDays);

                rBonus.setStorage(megaAchievements.getRewardStorageByAwardId(awardId));
                rBonus.setTransfer(megaAchievements.getRewardTransferByAwardId(awardId));

                referralBonuses.add(rBonus);
            } else {
                logDebug("MEGA_ACHIEVEMENT: " + type);
            }
        }
    }

    public void showInviteConfirmationDialog(){
        logDebug("showInviteConfirmationDialog");

        android.support.v7.app.AlertDialog.Builder builder = new android.support.v7.app.AlertDialog.Builder(this);
        LayoutInflater inflater = getLayoutInflater();
        View dialoglayout = inflater.inflate(R.layout.dialog_invite_friends_achievement, null);
        builder.setView(dialoglayout);
        successDialog = builder.create();
        successDialog.show();
    }

    public void showSnackbar(String s){
        showSnackbar(fragmentContainer, s);
    }

    @Override
    public void onRequestTemporaryError(MegaApiJava api, MegaRequest request, MegaError e) {

    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        logDebug("onActivityResult");
        if (requestCode == REQUEST_CODE_GET_CONTACTS && resultCode == RESULT_OK){
            logDebug("REQUEST_CODE_GET_CONTACTS");
            ArrayList<String> contacts = data.getStringArrayListExtra(AddContactActivityLollipop.EXTRA_CONTACTS);
            if (contacts != null){
                if (inviteFriendsFragment != null && inviteFriendsFragment.isAdded()){
                    for (int i=0; i<contacts.size(); i++){
                        inviteFriendsFragment.addMail(contacts.get(i));
                    }
                }
                else {
                    megaApi.getAccountAchievements(this);
                    pendingAttaches = true;
                    pendingContacts = contacts;
                    visibleFragment = INVITE_FRIENDS_FRAGMENT;
                    achievementType = -1;
                }
            }
        }
        else{
            super.onActivityResult(requestCode, resultCode, data);
        }
    }

    void attachPendig () {
        if (pendingContacts != null) {
            if (inviteFriendsFragment != null && inviteFriendsFragment.isAdded()) {
                for (int i = 0; i < pendingContacts.size(); i++) {
                    inviteFriendsFragment.addMail(pendingContacts.get(i));
                }
                pendingAttaches = false;
                pendingContacts.clear();
            }
        }
    }

    void setMails (ArrayList<String> mails){
        this.mails = mails;
    }

    ArrayList<String> getMails(){
        return mails;
    }
}
