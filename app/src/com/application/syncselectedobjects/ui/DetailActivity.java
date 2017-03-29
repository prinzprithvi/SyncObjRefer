/*
 * Copyright (c) 2014-present, salesforce.com, inc.
 * All rights reserved.
 * Redistribution and use of this software in source and binary forms, with or
 * without modification, are permitted provided that the following conditions
 * are met:
 * - Redistributions of source code must retain the above copyright notice, this
 * list of conditions and the following disclaimer.
 * - Redistributions in binary form must reproduce the above copyright notice,
 * this list of conditions and the following disclaimer in the documentation
 * and/or other materials provided with the distribution.
 * - Neither the name of salesforce.com, inc. nor the names of its contributors
 * may be used to endorse or promote products derived from this software without
 * specific prior written permission of salesforce.com, inc.
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 */
package com.application.syncselectedobjects.ui;

import org.json.JSONException;
import org.json.JSONObject;

import android.app.LoaderManager;
import android.content.Intent;
import android.content.Loader;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import com.application.syncselectedobjects.R;
import com.application.syncselectedobjects.loaders.ContactDetailLoader;
import com.application.syncselectedobjects.loaders.ContactListLoader;
import com.application.syncselectedobjects.objects.ContactObject;
import com.salesforce.androidsdk.accounts.UserAccount;
import com.salesforce.androidsdk.rest.RestClient;
import com.salesforce.androidsdk.smartstore.store.SmartStore;
import com.salesforce.androidsdk.smartsync.app.SmartSyncSDKManager;
import com.salesforce.androidsdk.smartsync.manager.SyncManager;
import com.salesforce.androidsdk.smartsync.util.Constants;
import com.salesforce.androidsdk.ui.SalesforceActivity;


/**
 * Object detail activity.
 *
 * @author bhariharan
 */
public class DetailActivity extends SalesforceActivity implements LoaderManager.LoaderCallbacks<ContactObject> {

	private static final int CONTACT_DETAIL_LOADER_ID = 2;
    private static final String TAG = "DetailActivity";

    private UserAccount curAccount;
    private String objectId;
    private String objectTitle;
    private ContactObject sObject;
    private DeleteDialogFragment deleteConfirmationDialog;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.detail);
		getActionBar().setHomeButtonEnabled(true);
		getActionBar().setIcon(R.drawable.ic_action_back);
		final Intent launchIntent = getIntent();
		if (launchIntent != null) {
			objectId = launchIntent.getStringExtra(MainActivity.OBJECT_ID_KEY);
			objectTitle = launchIntent.getStringExtra(MainActivity.OBJECT_TITLE_KEY);
			getActionBar().setTitle(launchIntent.getStringExtra(MainActivity.OBJECT_NAME_KEY));
			getActionBar().setSubtitle(objectTitle);
		}
		deleteConfirmationDialog = new DeleteDialogFragment();
	}

	@Override
	public void onResume(RestClient client) {
		curAccount = SmartSyncSDKManager.getInstance().getUserAccountManager().getCurrentUser();
		getLoaderManager().initLoader(CONTACT_DETAIL_LOADER_ID, null, this).forceLoad();
	}

	@Override
	public void onPause() {
		getLoaderManager().destroyLoader(CONTACT_DETAIL_LOADER_ID);
		super.onPause();
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
	    overridePendingTransition(R.anim.slide_in_from_right, R.anim.slide_out_to_left);
		final MenuInflater inflater = getMenuInflater();
	    inflater.inflate(R.menu.action_bar_menu, menu);
	    final MenuItem searchItem = menu.findItem(R.id.action_search);
	    searchItem.setVisible(false);
	    final MenuItem logoutItem = menu.findItem(R.id.action_logout);
	    logoutItem.setVisible(false);
	    final MenuItem addItem = menu.findItem(R.id.action_add);
	    addItem.setVisible(false);
	    final MenuItem refreshItem = menu.findItem(R.id.action_refresh);
	    refreshItem.setIcon(R.drawable.ic_action_save);
	    return super.onCreateOptionsMenu(menu);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
	    switch (item.getItemId()) {
	    	case android.R.id.home:
	    		finish();
	    	    overridePendingTransition(R.anim.slide_in_from_left, R.anim.slide_out_to_right);
	    		return true;
	        case R.id.action_refresh:
	        	save();
	            return true;
	        default:
	            return super.onOptionsItemSelected(item);
	    }
	}

	@Override
	public Loader<ContactObject> onCreateLoader(int id, Bundle args) {
		return new ContactDetailLoader(this, curAccount, objectId);
	}

	@Override
	public void onLoadFinished(Loader<ContactObject> loader,
			ContactObject data) {
		sObject = data;
		refreshScreen();
	}

	@Override
	public void onLoaderReset(Loader<ContactObject> loader) {
		sObject = null;
		refreshScreen();
	}

	/**
	 * Callback received when the 'Delete' button is clicked.
	 *
	 * @param v View that was clicked.
	 */
	public void onDeleteClicked(View v) {
		deleteConfirmationDialog.show(getFragmentManager(), "DeleteDialog");
	}

	/**
	 * Performs the underlying deletion of a record.
	 */
	public void reallyDelete() {
		final SmartStore smartStore = SmartSyncSDKManager.getInstance().getSmartStore(curAccount);
		JSONObject contact;
		try {
			contact = smartStore.retrieve(ContactListLoader.CONTACT_SOUP,
					smartStore.lookupSoupEntryId(ContactListLoader.CONTACT_SOUP,
					Constants.ID, objectId)).getJSONObject(0);
			contact.put(SyncManager.LOCAL, true);
			contact.put(SyncManager.LOCALLY_DELETED, true);
			smartStore.upsert(ContactListLoader.CONTACT_SOUP, contact);
			Toast.makeText(this, "Delete successful!", Toast.LENGTH_LONG).show();
			finish();
		} catch (JSONException e) {
            Log.e(TAG, "JSONException occurred while parsing", e);
		}
	}

	private void refreshScreen() {
		if (sObject != null) {
			setText((EditText) findViewById(R.id.first_name_field),
					sObject.getFirstName());
			setText((EditText) findViewById(R.id.last_name_field),
					sObject.getLastName());
			setText((EditText) findViewById(R.id.title_field),
					sObject.getTitle());
			setText((EditText) findViewById(R.id.phone_field),
					sObject.getPhone());
			setText((EditText) findViewById(R.id.email_field),
					sObject.getEmail());
			setText((EditText) findViewById(R.id.department_field),
					sObject.getDepartment());
			setText((EditText) findViewById(R.id.home_phone_field),
					sObject.getHomePhone());
		}
	}

	private void setText(EditText textField, String text) {
		if (textField != null) {
			textField.setText(text);
		}
	}

	private void save() {
		final String firstName = ((EditText) findViewById(R.id.first_name_field)).getText().toString();
		final String lastName = ((EditText) findViewById(R.id.last_name_field)).getText().toString();
		if (TextUtils.isEmpty(firstName) || TextUtils.isEmpty(lastName)) {
			Toast.makeText(this, "First and last name cannot be empty!", Toast.LENGTH_LONG).show();
			return;
		}
		final String title = ((EditText) findViewById(R.id.title_field)).getText().toString();
		final String phone = ((EditText) findViewById(R.id.phone_field)).getText().toString();
		final String email = ((EditText) findViewById(R.id.email_field)).getText().toString();
		final String department = ((EditText) findViewById(R.id.department_field)).getText().toString();
		final String homePhone = ((EditText) findViewById(R.id.home_phone_field)).getText().toString();
		final SmartStore smartStore = SmartSyncSDKManager.getInstance().getSmartStore(curAccount);
		JSONObject contact;
		try {
			boolean isCreate = TextUtils.isEmpty(objectId);
			if (!isCreate) {
				contact = smartStore.retrieve(ContactListLoader.CONTACT_SOUP,
						smartStore.lookupSoupEntryId(ContactListLoader.CONTACT_SOUP,
						Constants.ID, objectId)).getJSONObject(0);
			} else {
				contact = new JSONObject();
				contact.put(Constants.ID, "local_" + System.currentTimeMillis()
						+ Constants.EMPTY_STRING);
				final JSONObject attributes = new JSONObject();
				attributes.put(Constants.TYPE.toLowerCase(), Constants.CONTACT);
				contact.put(Constants.ATTRIBUTES, attributes);
			}
			contact.put(ContactObject.FIRST_NAME, firstName);
			contact.put(ContactObject.LAST_NAME, lastName);
			contact.put(ContactObject.TITLE, title);
			contact.put(ContactObject.PHONE, phone);
			contact.put(ContactObject.EMAIL, email);
			contact.put(ContactObject.DEPARTMENT, department);
			contact.put(ContactObject.HOME_PHONE, homePhone);
			contact.put(SyncManager.LOCAL, true);
			contact.put(SyncManager.LOCALLY_UPDATED, !isCreate);
			contact.put(SyncManager.LOCALLY_CREATED, isCreate);
			contact.put(SyncManager.LOCALLY_DELETED, false);
			if (isCreate) {
				smartStore.create(ContactListLoader.CONTACT_SOUP, contact);
			} else {
				smartStore.upsert(ContactListLoader.CONTACT_SOUP, contact);
			}
			Toast.makeText(this, "Save successful!", Toast.LENGTH_LONG).show();
			finish();
		} catch (JSONException e) {
            Log.e(TAG, "JSONException occurred while parsing", e);
		}
	}
}
