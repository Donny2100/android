package mega.privacy.android.app.utils;

import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.io.File;
import java.util.ArrayList;
import java.util.concurrent.CopyOnWriteArrayList;

import mega.privacy.android.app.MegaApplication;
import mega.privacy.android.app.MimeTypeList;
import mega.privacy.android.app.R;
import mega.privacy.android.app.listeners.ExportListener;
import mega.privacy.android.app.lollipop.WebViewActivityLollipop;
import nz.mega.sdk.MegaNode;

import static mega.privacy.android.app.utils.Constants.*;
import static mega.privacy.android.app.utils.FileUtils.*;
import static mega.privacy.android.app.utils.LogUtil.*;
import static mega.privacy.android.app.utils.TextUtil.*;
import static mega.privacy.android.app.utils.Util.*;

public class MegaNodeUtil {

    /**
     * The method to calculate how many nodes are folders in array list
     *
     * @param nodes the nodes to be calculated
     * @return how many nodes are folders in array list
     */
    public static int getNumberOfFolders(ArrayList<MegaNode> nodes) {

        int folderCount = 0;

        if (nodes == null) return folderCount;

        CopyOnWriteArrayList<MegaNode> safeList = new CopyOnWriteArrayList(nodes);

        for (MegaNode node : safeList) {
            if (node == null) {
                safeList.remove(node);
            } else if (node.isFolder()) {
                folderCount++;
            }
        }

        nodes = new ArrayList<>(safeList);
        return folderCount;
    }

    /**
     * @param node the detected node
     * @return whether the node is taken down
     */
    public static boolean isNodeTakenDown(MegaNode node) {
        return node != null && node.isTakenDown();
    }


    /**
     * If the node is taken down, and try to execute action against the node,
     * such as manage link, remove link, show the alert dialog
     *
     * @param node the detected node
     * @return whether show the dialog for the mega node or not
     */
    public static boolean showTakenDownNodeActionNotAvailableDialog(MegaNode node, Context context) {
        if (isNodeTakenDown(node)) {
            showSnackbar(context, context.getString(R.string.error_download_takendown_node));
            return true;
        } else {
            return false;
        }
    }

    /**
     * The static class to show taken down notice for mega node when the node is taken down and try to be opened in the preview
     */
    public static class NodeTakenDownAlertHandler {
        /**
         * alertTakenDown is the dialog to be shown. It resides inside this static class to prevent multiple definition within the activity class
         */
        private static AlertDialog alertTakenDown = null;

        public static class TakenDownAlertFragment extends DialogFragment {
            @Override
            public Dialog onCreateDialog(Bundle savedInstanceState) {
                AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(getActivity());
                dialogBuilder.setTitle(getActivity().getString(R.string.general_not_available))
                             .setMessage(getActivity().getString(R.string.error_download_takendown_node)).setNegativeButton(R.string.general_dismiss, (dialog, i) -> {
                    dialog.dismiss();
                    getActivity().finish();
                });
                alertTakenDown = dialogBuilder.create();

                setCancelable(false);

                return alertTakenDown;
            }
        }

        /**
         * @param activity the activity is the page where dialog is shown
         */
        public static void showTakenDownAlert(final AppCompatActivity activity) {

            if (activity == null
                    || activity.isFinishing()
                    || (alertTakenDown != null && alertTakenDown.isShowing())) {
                return;
            }

            new TakenDownAlertFragment().show(activity.getSupportFragmentManager(), "taken_down");
        }
    }

    /**
     * The static class to show taken down dialog for mega node when the node is taken down and be clicked in adapter
     */
    public static class NodeTakenDownDialogHandler {

        /**
         * The listener to handle button click events
         */
        public interface nodeTakenDownDialogListener {
            void onOpenClicked(int currentPosition, View view);

            void onDisputeClicked();

            void onCancelClicked();
        }

        /**
         * show dialog
         *
         * @param isFolder        the clicked node
         * @param view            the view in the adapter which triggers the click event
         * @param currentPosition the view position in adapter
         * @param listener        the listener to handle all clicking event
         * @param context         the context where adapter resides
         * @return the dialog object to be handled by adapter to be dismissed, in case of window leaking situation
         */
        public static AlertDialog showTakenDownDialog(boolean isFolder, final View view, final int currentPosition, nodeTakenDownDialogListener listener, Context context) {
            int alertMessageID = isFolder ? R.string.message_folder_takedown_pop_out_notification : R.string.message_file_takedown_pop_out_notification;

            AlertDialog.Builder builder = new AlertDialog.Builder(context);
            LayoutInflater inflater = LayoutInflater.from(context);
            View v = inflater.inflate(R.layout.dialog_three_vertical_buttons, null);
            builder.setView(v);

            TextView title = v.findViewById(R.id.dialog_title);
            TextView text = v.findViewById(R.id.dialog_text);

            Button openButton = v.findViewById(R.id.dialog_first_button);
            Button disputeButton = v.findViewById(R.id.dialog_second_button);
            Button cancelButton = v.findViewById(R.id.dialog_third_button);

            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT);
            params.gravity = Gravity.RIGHT;

            title.setText(R.string.general_error_word);
            text.setText(alertMessageID);
            openButton.setText(R.string.context_open_link);
            disputeButton.setText(R.string.dispute_takendown_file);
            cancelButton.setText(R.string.general_cancel);

            final AlertDialog dialog = builder.create();
            dialog.setCancelable(false);
            dialog.setCanceledOnTouchOutside(false);

            openButton.setOnClickListener(button -> {
                listener.onOpenClicked(currentPosition, view);
                dialog.dismiss();
            });

            disputeButton.setOnClickListener(button -> {
                listener.onDisputeClicked();
                Intent openTermsIntent = new Intent(context, WebViewActivityLollipop.class);
                openTermsIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                openTermsIntent.setData(Uri.parse(DISPUTE_URL));
                context.startActivity(openTermsIntent);
                dialog.dismiss();
            });

            cancelButton.setOnClickListener(button -> {
                listener.onCancelClicked();
                dialog.dismiss();
            });

            dialog.show();

            return dialog;
        }
    }

    public static void shareNode (Context context, MegaNode node) {
        if (shouldContinueWithoutError(context, "sharing node", node)) {
            String path = getLocalFile(context, node.getName(), node.getSize());

            Intent shareIntent = new Intent(android.content.Intent.ACTION_SEND);

            if (!isTextEmpty(path)) {
                Uri uri = getUriForFile(context, new File(path));
                shareIntent.setType(MimeTypeList.typeForName(node.getName()).getType() + "/*");
                shareIntent.putExtra(Intent.EXTRA_STREAM, uri);
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                }
                context.startActivity(Intent.createChooser(shareIntent, context.getString(R.string.context_share)));
            } else if (node.isExported()) {
                startShareIntent(context, shareIntent, node.getPublicLink());
            } else {
                MegaApplication.getInstance().getMegaApi().exportNode(node, new ExportListener(context, shareIntent));
            }
        }
    }

    public static void startShareIntent (Context context, Intent shareIntent, String link) {
        shareIntent.setType("text/plain");
        shareIntent.putExtra(Intent.EXTRA_TEXT, link);
        context.startActivity(Intent.createChooser(shareIntent, context.getString(R.string.context_share)));
    }

    private static boolean shouldContinueWithoutError(Context context, String message, MegaNode node) {
        String error = "Error " + message + ". ";

        if (node == null) {
            logError(error + "Node == NULL");
            return false;
        } else if (!isOnline(context)) {
            logError(error + "No network connection");
            showSnackbar(context, context.getString(R.string.error_server_connection_problem));
            return false;
        }

        return true;
    }
}
