package org.nerdslot.Fragments.Admin;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.ProgressBar;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProviders;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.switchmaterial.SwitchMaterial;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.database.DatabaseReference;
import com.squareup.picasso.Picasso;

import org.jetbrains.annotations.NotNull;
import org.nerdslot.Foundation.FireUtil;
import org.nerdslot.Foundation.Helper.IndexList;
import org.nerdslot.Foundation.Helper.Upload;
import org.nerdslot.Fragments.Admin.Impl.CreateIssueInterface;
import org.nerdslot.Models.Category;
import org.nerdslot.Models.Issue.Issue;
import org.nerdslot.R;
import org.nerdslot.ViewModels.CategoryViewModel;

import java.util.ArrayList;

import static android.app.Activity.RESULT_OK;

/**
 * A simple {@link Fragment} subclass.
 * Activities that contain this fragment must implement the
 * {@link AdminInterface} interface
 * to handle interaction events.
 */
public class CreateIssue extends Fragment implements CreateIssueInterface {

    private AdminInterface mListener;
    private AppCompatActivity activity;

    private TextInputLayout titleInputLayout, priceInputLayout, categorySpinnerLayout;
    private TextInputEditText issueTitleTextView, issueDescTextView, issuePriceTextView;
    private AutoCompleteTextView categorySpinner;
    private SwitchMaterial isFeaturedSwitch, isFreeSwitch;
    private ImageView coverImage;
    private MaterialButton coverUploadBtn, selectFileBtn, createIssueBtn;
    private ProgressBar coverUploadProgressBar, fileUploadProgressBar;
    private View[] viewGroup;

    private Issue issue;
    private IndexList<Category> categories;
    private ArrayList<String> categoryNames;
    private String id, title, description, category_id, currency, price, coverUrl;
    private boolean isFeatured, isFree;

    private Upload upload;
    private DatabaseReference issueReference = FireUtil.databaseReference(Issue.class);

    public CreateIssue() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null)
            issue = CreateIssueArgs.fromBundle(getArguments()).getIssue();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_create_issue, container, false);
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        CategoryViewModel categoryViewModel = ViewModelProviders.of(activity).get(CategoryViewModel.class);
        categoryViewModel.all().observe(this, categories -> {
            this.categories = new IndexList<>(Category::getName);

            categoryNames = new ArrayList<>();
            categoryNames.clear();

            for (Category category : categories) {
                categoryNames.add(category.getName());
                this.categories.add(category);
            }

            populateCategorySpinner(categoryNames);
        });
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        findViewsById(view);

        coverUploadBtn.setOnClickListener(this);
        selectFileBtn.setOnClickListener(this);
        createIssueBtn.setOnClickListener(this);
        coverImage.setOnClickListener(this);

        setupListeners();
        upload = new Upload(this);
    }

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        activity = ((AppCompatActivity) getActivity());

        if (context instanceof org.nerdslot.Fragments.Admin.AdminInterface)
            mListener = (org.nerdslot.Fragments.Admin.AdminInterface) context;
        else throw new RuntimeException(context.toString()
                + " must implement AdminInterface");
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;
    }

    private void findViewsById(@NotNull View view) {
        categorySpinnerLayout = view.findViewById(R.id.category_spinner_layout);
        categorySpinner = view.findViewById(R.id.categories_spinner);

        titleInputLayout = view.findViewById(R.id.issue_title_input_layout);
        priceInputLayout = view.findViewById(R.id.issue_price_input_layout);

        issueTitleTextView = view.findViewById(R.id.issue_title_text_edit);
        issueDescTextView = view.findViewById(R.id.issue_description_text_edit);
        issuePriceTextView = view.findViewById(R.id.issue_price_text_edit);

        coverImage = view.findViewById(R.id.cover_image);
        coverUploadBtn = view.findViewById(R.id.cover_upload_btn);
        coverUploadProgressBar = view.findViewById(R.id.cover_upload_progress_bar);

        selectFileBtn = view.findViewById(R.id.select_file_btn);
        fileUploadProgressBar = view.findViewById(R.id.file_upload_progress_bar);

        isFeaturedSwitch = view.findViewById(R.id.is_featured_switch);
        isFreeSwitch = view.findViewById(R.id.is_free_switch);

        createIssueBtn = view.findViewById(R.id.create_issue_btn);

        viewGroup = new View[]{categorySpinnerLayout, titleInputLayout, priceInputLayout};
    }

    private void populateCategorySpinner(ArrayList<String> categoryNames) {

        ArrayAdapter<String> categoriesAdapter = new ArrayAdapter<>(activity, R.layout.spinner_item, categoryNames);

        categorySpinner.setAdapter(categoriesAdapter);
    }

    private void validate() {
        resetError(viewGroup);
        setEnabled(createIssueBtn, false); //Disable Button

        boolean cancel = false;
        View focusView = null;

        title = String.valueOf(issueTitleTextView.getText());
        description = String.valueOf(issueDescTextView.getText());
        price = String.valueOf(issuePriceTextView.getText());

        if (TextUtils.isEmpty(title)) {
            setError(titleInputLayout);
            focusView = issueTitleTextView;
            cancel = true;
        } else if (TextUtils.isEmpty(price) && !isFreeSwitch.isChecked()) {
            setError(priceInputLayout);
            focusView = issuePriceTextView;
            cancel = true;
        } else if (TextUtils.isEmpty(category_id)) {
            setError(categorySpinnerLayout);
            cancel = true;
        } else if (upload.epubUri == null) {
            sendToast(activity, "Please select Epub!");
            cancel = true;
        } else if (upload.imageUri == null) {
            sendToast(activity, "Select a Cover Image first!");
            cancel = true;
        }

        if (!cancel) {
            createIssue();
        } else {
            // There was an error; don't create Issue
            // form field with an error.
            if (focusView != null) focusView.requestFocus();
            setEnabled(createIssueBtn, true); // Enable Button if errors exists
        }
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.create_issue_btn: {
                validate();
                break;
            }

            case R.id.cover_upload_btn:
            case R.id.cover_image: {
                upload.__construct(MIME_TYPE.IMAGE);
                break;
            }

            case R.id.select_file_btn: {
                upload.__construct(MIME_TYPE.EPUB);
                break;
            }
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == SELECT_FILE_REQUEST_CODE && resultCode == RESULT_OK && data != null && data.getData() != null) {
            Uri uri = data.getData();

            if (upload.getMimeType().equals(MIME_TYPE.IMAGE.toString())) {
                upload.imageUri = uri;
                coverUploadBtn.setVisibility(View.GONE);
                coverImage.setVisibility(View.VISIBLE);

                coverImage.setBackground(ContextCompat.getDrawable(activity, R.drawable.pre_load_image));
                coverImage.requestLayout();

                Picasso.with(activity)
                        .load(uri)
                        .into(coverImage);
            } else if (upload.getMimeType().equals(MIME_TYPE.EPUB.toString())) {
                upload.epubUri = uri;

                String[] segments = uri.getPath().split("/");
                String lastSegment = segments[segments.length - 1];

                selectFileBtn.setIcon(null);
                selectFileBtn.setText(String.format("%s", lastSegment));
            }
        }
    }

    @Override
    public void onCheckedChanged(CompoundButton compoundButton, boolean isChecked) {
        switch (compoundButton.getId()) {
            case R.id.is_featured_switch: {
                isFeatured = isChecked;
                break;
            }
            case R.id.is_free_switch: {
                isFree = isChecked;
                issuePriceTextView.setText(isChecked ? "FREE" : "");
                setEnabled(priceInputLayout, !isChecked);
                setVisibility(priceInputLayout, isChecked ? View.GONE : View.VISIBLE);
                break;
            }
        }
    }

    private void setupListeners() {
        isFeaturedSwitch.setOnCheckedChangeListener(this);
        isFreeSwitch.setOnCheckedChangeListener(this);

        issueTitleTextView.addTextChangedListener(this);
        issuePriceTextView.addTextChangedListener(this);

        issueTitleTextView.setOnFocusChangeListener(this);
        issuePriceTextView.setOnFocusChangeListener(this);

        categorySpinner.setOnItemClickListener((adapterView, view, i, l) -> {
            resetError(categorySpinnerLayout);
            String categoryName = adapterView.getItemAtPosition(i).toString();
            Category category = categories.get(categories.indexOf(categoryName));
            category_id = category.getId();
        });
    }

    private void createIssue() {
        String key = issueReference.push().getKey();
        issue = new Issue.Builder()
                .setId(key)
                .setCategory_id(category_id)
                .setMagazine_id(upload.getMagazineSessionKey())
                .setTitle(title)
                .setDescription(description)
                .setCurrency(currency)
                .setPrice(price)
                .setFeatured(isFeatured)
                .setIssueImageUri("null")
                .setRateCount(0.0)
                .build();

        issueReference.child(key).setValue(issue);

        upload.cover__(title, issue.getId(), coverUploadProgressBar, coverImage, coverUploadBtn);
        upload.magazine__(title, fileUploadProgressBar, selectFileBtn);

        resetViews(issueTitleTextView, issuePriceTextView);
        resetView(issueDescTextView, "No description available!");
//        upload = new Upload(this);
        setEnabled(createIssueBtn, true);
    }

    @Override
    public void onFocusChange(View view, boolean hasFocus) {
        switch (view.getId()) {
            case R.id.issue_title_text_edit: {
                if (!hasFocus)
                    validateTextInput(titleInputLayout, String.valueOf(((TextInputEditText) view).getText()));
                break;
            }
            case R.id.issue_price_text_edit: {
                if (!hasFocus)
                    validateTextInput(priceInputLayout, String.valueOf(((TextInputEditText) view).getText()));
                break;
            }
        }
    }

    @Override
    public void afterTextChanged(Editable editable) {
        if (issueTitleTextView.getText().hashCode() == editable.hashCode())
            resetError(titleInputLayout);
        else if (issuePriceTextView.getText().hashCode() == editable.hashCode())
            resetError(priceInputLayout);
    }
}
