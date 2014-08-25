package com.tomclaw.mandarin.main;

import android.app.ActionBar;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.Spinner;
import android.widget.TextView;
import com.tomclaw.mandarin.R;
import com.tomclaw.mandarin.core.GlobalProvider;
import com.tomclaw.mandarin.im.Gender;
import com.tomclaw.mandarin.im.icq.IcqSearchOptionsBuilder;
import com.tomclaw.mandarin.main.views.AgePickerView;

/**
 * Created by Igor on 26.06.2014.
 */
public class SearchActivity extends ChiefActivity {

    private int accountDbId;

    private TextView keywordName;
    private AgePickerView agePickerView;
    private Spinner genderSpinner;
    private CheckBox onlineBox;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        accountDbId = getIntentAccountDbId(getIntent());
        if (accountDbId == -1) {
            finish();
            return;
        }

        ActionBar actionBar = getActionBar();
        actionBar.setDisplayHomeAsUpEnabled(true);
        actionBar.setHomeButtonEnabled(true);

        setContentView(R.layout.search_activity);

        ArrayAdapter<String> adapter = new ArrayAdapter<String>(this, R.layout.gender_spinner_item,
                getResources().getStringArray(R.array.gender_spinner_strings));
        adapter.setDropDownViewResource(R.layout.gender_spinner_dropdown_item);
        Spinner genderSelector = (Spinner) findViewById(R.id.gender_selector);
        genderSelector.setAdapter(adapter);

        keywordName = (TextView) findViewById(R.id.keyword_edit);
        agePickerView = (AgePickerView) findViewById(R.id.age_range);
        genderSpinner = (Spinner) findViewById(R.id.gender_selector);
        onlineBox = (CheckBox) findViewById(R.id.online_check);
    }

    @Override
    public boolean onCreateOptionsMenu(final Menu menu) {
        getMenuInflater().inflate(R.menu.search_activity_menu, menu);
        final MenuItem item = menu.findItem(R.id.search_action_menu);
        item.getActionView().setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                menu.performIdentifierAction(item.getItemId(), 0);
            }
        });
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home: {
                finish();
                break;
            }
            case R.id.search_action_menu: {
                doSearch();
                break;
            }
        }
        return true;
    }

    private int getIntentAccountDbId(Intent intent) {
        Bundle bundle = intent.getExtras();
        int accountDbId = -1;
        // Checking for bundle condition.
        if (bundle != null) {
            accountDbId = bundle.getInt(GlobalProvider.ROSTER_BUDDY_ACCOUNT_DB_ID, accountDbId);
        }
        return accountDbId;
    }

    @Override
    public void onCoreServiceIntent(Intent intent) {
    }

    private void doSearch() {
        IcqSearchOptionsBuilder builder = new IcqSearchOptionsBuilder(System.currentTimeMillis());
        String keyword = keywordName.getText().toString();
        // Obtain search builder instance from account.
        builder.keyword(keyword);
        builder.online(onlineBox.isChecked());
        if (!agePickerView.isAnyAge()) {
            builder.age(agePickerView.getValueMin(), agePickerView.getValueMax());
        }
        String selectedGender = (String) genderSpinner.getSelectedItem();
        if (TextUtils.equals(selectedGender, getString(R.string.gender_female))) {
            builder.gender(Gender.Female);
        } else if (TextUtils.equals(selectedGender, getString(R.string.gender_male))) {
            builder.gender(Gender.Male);
        }

        Intent intent = new Intent(SearchActivity.this, SearchResultActivity.class);
        intent.putExtra(SearchResultActivity.SEARCH_OPTIONS, builder);
        intent.putExtra(GlobalProvider.ROSTER_BUDDY_ACCOUNT_DB_ID, accountDbId);
        startActivity(intent);
    }
}