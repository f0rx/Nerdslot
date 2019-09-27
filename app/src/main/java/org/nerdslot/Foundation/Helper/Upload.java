package org.nerdslot.Foundation.Helper;

import android.content.Intent;
import android.net.Uri;
import android.view.View;
import android.widget.ImageView;
import android.widget.ProgressBar;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.google.android.material.button.MaterialButton;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.storage.StorageMetadata;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import org.jetbrains.annotations.NotNull;
import org.nerdslot.Foundation.Reference;
import org.nerdslot.Fragments.RootInterface;
import org.nerdslot.Models.Issue.Issue;
import org.nerdslot.Models.Issue.Magazine;
import org.nerdslot.R;

public class Upload implements RootInterface {
    public Uri imageUri;
    public Uri epubUri;
    private Fragment fragment;
    private String fileExtension;
    private String mime;
    private String sessionKey;
    private String epubStringUri;
    private String imageStringUri;

    public Upload(@NonNull Fragment context) {
        this.fragment = context;
    }

    public void __construct(@NotNull MIME_TYPE mimeType) {
        setMimeType(mimeType);
        startIntent();

        DatabaseReference magazineRef = new Reference.Builder()
                .setNode(Magazine.class)
                .getDatabaseReference();
        sessionKey = magazineRef.push().getKey();
    }

    /**
     * Returns the Mime Type to String
     *
     * @return String File Mime-Type
     */
    public String getMimeType() {
        return mime;
    }

    private void setMimeType(@NotNull MIME_TYPE mimeType) {
        switch (mimeType) {
            case JPG:
                mime = MIME_TYPE.JPG.toString();
                setExtension(mimeType);
                break;
            case PNG:
                mime = MIME_TYPE.PNG.toString();
                setExtension(mimeType);
                break;
            case IMAGE:
                mime = MIME_TYPE.IMAGE.toString();
                setExtension(mimeType);
                break;
            case EPUB:
                mime = MIME_TYPE.EPUB.toString();
                setExtension(mimeType);
                break;
            case PDF:
                mime = MIME_TYPE.PDF.toString();
                setExtension(mimeType);
                break;
            case DOC:
                mime = MIME_TYPE.DOC.toString();
                setExtension(mimeType);
                break;
            case TXT:
                mime = MIME_TYPE.TXT.toString();
                setExtension(mimeType);
                break;
            case ZIP:
                mime = MIME_TYPE.ZIP.toString();
                setExtension(mimeType);
                break;
            default:
                mime = "*/*";
                setExtension(mimeType);
        }
    }

    public String getExtension() {
        return fileExtension;
    }

    private void setExtension(@NotNull MIME_TYPE mimeType) {
        switch (mimeType) {
            case PNG:
                fileExtension = ".png";
                break;
            case JPG:
            case IMAGE:
                fileExtension = ".jpg";
                break;
            case DOC:
                fileExtension = ".doc";
                break;
            case EPUB:
                fileExtension = ".epub";
                break;
            case PDF:
                fileExtension = ".pdf";
                break;
            case TXT:
                fileExtension = ".txt";
                break;
            case ZIP:
                fileExtension = ".zip";
                break;
            default:
                fileExtension = ".file";
        }
    }

    private void startIntent() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType(mime);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.putExtra(Intent.EXTRA_LOCAL_ONLY, true);
        fragment.startActivityForResult(Intent.createChooser(intent, "Choose File"), SELECT_FILE_REQUEST_CODE);
    }

    public void magazine__(String title, View[] viewGroup) {
        setMimeType(MIME_TYPE.EPUB); // Set the File Extension

        ProgressBar progressBar = (ProgressBar) viewGroup[0];
        MaterialButton button = (MaterialButton) viewGroup[1];
        setVisibility(button, View.GONE); // Hide "Select File" Button
        setVisibility(progressBar, View.VISIBLE); // Show Progress Bar

        StorageReference uploadReference = new Reference.Builder()
                .setNode(Magazine.class)
                .setNode(getSessionKey())
                .setNode(title + getExtension())
                .getStorageReference(); // Get Storage Reference to insert Epub File

        StorageMetadata magazineMetadata = new StorageMetadata.Builder()
                .setContentType(getMimeType())
                .setCustomMetadata("Nerdslot", "Magazines")
                .build(); // Create Metadata

        UploadTask uploadTask = uploadReference.putFile(epubUri, magazineMetadata);
        uploadTask.continueWithTask(task -> {
            if (!task.isSuccessful()) throw task.getException();

            return uploadReference.getDownloadUrl();
        }).addOnCompleteListener(task -> task.addOnSuccessListener(uri -> {
            epubStringUri = uri.getLastPathSegment();
            updateMagazine(title); // Update Magazine Details

            setVisibility(button, View.VISIBLE); // Show "Select File" Button
            setVisibility(progressBar, View.GONE); // Hide Progress Bar
            button.setIcon(ContextCompat.getDrawable(fragment.getContext(), R.drawable.ic_upload_ios)); // Set Icon from Button
            button.setIconGravity(MaterialButton.ICON_GRAVITY_START); // Set Icon Gravity
            button.setIconTintResource(R.color.black); // Set Icon TINT
            button.setText(String.format("%s", fragment.getString(R.string.select_file_string))); // Set Text
            reset();
        }).addOnFailureListener(e -> sendResponse(e.getMessage(), e)));
    }

    public void cover__(String title, String issueId, View[] viewGroup) {
        setMimeType(MIME_TYPE.JPG); // Set the File Extension

        ProgressBar progressBar = (ProgressBar) viewGroup[0];
        ImageView imageView = (ImageView) viewGroup[1];
        MaterialButton button = (MaterialButton) viewGroup[2];
        setVisibility(progressBar, View.VISIBLE); // Show Progress Bar
        setVisibility(imageView, View.GONE); // Hide ImageView
        setEnabled(imageView, false); // Disable Button

        StorageReference uploadReference = new Reference.Builder()
                .setNode(Magazine.class)
                .setNode(getSessionKey())
                .setNode(MAGAZINE_COVER_NODE)
                .setNode(title + getExtension())
                .getStorageReference(); // Get Storage Reference to insert image

        StorageMetadata coverMetadata = new StorageMetadata.Builder()
                .setContentType(getMimeType())
                .setCustomMetadata("Nerdslot", "Magazines")
                .setCustomMetadata("Type", "magazine-cover-image")
                .build(); // Create Metadata

        UploadTask uploadTask = uploadReference.putFile(imageUri, coverMetadata);
        uploadTask.continueWithTask(task -> {
            if (!task.isSuccessful()) throw task.getException();

            return uploadReference.getDownloadUrl();
        }).addOnCompleteListener(task -> task.addOnSuccessListener(uri -> {
            imageStringUri = uri.getLastPathSegment(); // Get Uri last Segment
            updateMagazine(title); // Update Magazine Info

            // Update Issue Image Uri
            new Reference.Builder()
                    .setNode(Issue.class)
                    .setNode(issueId)
                    .setNode("issueImageUri")
                    .getDatabaseReference()
                    .setValue(imageStringUri);

            setVisibility(progressBar, View.GONE); // Hide Progress Bar
            setVisibility(button, View.VISIBLE); // Show Cover Upload Button (For new Upload)
            setEnabled(imageView, true); // Enable Image View
            reset();
        }).addOnFailureListener(e -> sendResponse(e.getMessage(), e)));
    }

    private void updateMagazine(String title) {
        DatabaseReference reference = new Reference.Builder()
                .setNode(Magazine.class).setNode(getSessionKey()).getDatabaseReference();

        Magazine magazine = new Magazine.Builder()
                .setId(getSessionKey())
                .setTitle(title)
                .setMagazineUri(epubStringUri)
                .setCoverUri(imageStringUri)
                .setSlug(new Slugify.Builder().make(title))
                .build();

        reference.setValue(magazine);
    }

    public String getSessionKey() {
        return sessionKey;
    }

    private void reset(){
        imageUri = null;
        epubUri = null;
        fileExtension = null;
        mime = null;
        sessionKey = null;
        epubStringUri = null;
        imageStringUri = null;
    }
}
