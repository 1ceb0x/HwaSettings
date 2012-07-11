package com.cyanogenmod.settings.device.hwa;

import java.io.File;
import java.io.IOException;

import android.app.ListFragment;
import android.app.LoaderManager;
import android.content.Context;
import android.content.CursorLoader;
import android.content.Loader;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.CheckBox;
import android.widget.ListView;
import android.widget.SearchView;
import android.widget.SimpleCursorAdapter;
import android.widget.TextView;
import android.widget.Toast;

public class PackageListFragment extends ListFragment implements
		LoaderManager.LoaderCallbacks<Cursor>, SearchView.OnQueryTextListener {

	protected static final String TAG = "PackageListFragment";
	private static final int PACKAGE_LIST_LOADER = 0;
	private PackageListAdapater adapter;
	private SearchView mSearchView;
	protected Context mContext;
	private String query = "";
	private ListView mListView;

	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);
		setListShown(false);
		mContext = getActivity();
		mListView = (ListView) getListView();
		mSearchView = (SearchView) getActivity().findViewById(
				R.id.hwa_package_list_search_view);
		mListView.setTextFilterEnabled(true);
		mListView.setOnItemClickListener(listener);
		mSearchView.setOnQueryTextListener(this);
		mSearchView.setSubmitButtonEnabled(false);
		mSearchView.setEnabled(false);
		mSearchView.setFocusable(false);
		mSearchView.setClickable(false);
		new ScanForPackages().execute();
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		String[] from = new String[] { PackageListProvider.APPLICATION_LABEL,
				PackageListProvider.PACKAGE_NAME,
				PackageListProvider.HWA_DISABLED, PackageListProvider._ID };
		int[] to = new int[] { R.id.hwa_settings_name,
				R.id.hwa_settings_packagename, R.id.hwa_settings_blocked };
		adapter = new PackageListAdapater(getActivity(),
				R.layout.hwa_settings_row, null, from, to,
				SimpleCursorAdapter.FLAG_REGISTER_CONTENT_OBSERVER);
		setListAdapter(adapter);
	}

	private void startLoading() {
		adapter.notifyDataSetChanged();
		getListView().invalidateViews();
		getLoaderManager().initLoader(PACKAGE_LIST_LOADER, null, this);
	}

	private void restartLoading() {
		getLoaderManager().restartLoader(PACKAGE_LIST_LOADER, null, this);
	}

	@Override
	public Loader<Cursor> onCreateLoader(int id, Bundle args) {
		CursorLoader cursorLoader = new CursorLoader(getActivity(),
				PackageListProvider.CONTENT_URI, null,
				PackageListProvider.APPLICATION_LABEL + " LIKE '%" + query
						+ "%'", null, null);
		return cursorLoader;
	}

	@Override
	public void onLoadFinished(Loader<Cursor> cursorLoader, Cursor cursor) {
		adapter.swapCursor(cursor);
		setListShown(true);
		mSearchView.setEnabled(true);
		mSearchView.setFocusable(true);
		mSearchView.setClickable(true);
	}

	@Override
	public void onLoaderReset(Loader<Cursor> arg0) {
		adapter.swapCursor(null);
	}

	@Override
	public boolean onQueryTextChange(String newText) {
		query = newText;
		if (newText.length() > 0)
			mListView.setFilterText(newText);
		else
			mListView.clearTextFilter();
		restartLoading();
		return false;
	}

	@Override
	public boolean onQueryTextSubmit(String query) {
		return false;
	}

	private class ScanForPackages extends AsyncTask<Void, Void, Void> {

		@Override
		protected Void doInBackground(Void... params) {
			mContext.getContentResolver().insert(
					Uri.parse("content://" + PackageListProvider.AUTHORITY
							+ "/" + PackageListProvider.BASE_PATH + "/scan"),
					null);
			return null;
		}

		@Override
		protected void onPostExecute(Void result) {
			startLoading();
		}
	}

	private OnItemClickListener listener = new OnItemClickListener() {

		@Override
		public void onItemClick(AdapterView<?> parent, View view, int position,
				long id) {
			CheckBox cb = (CheckBox) view
					.findViewById(R.id.hwa_settings_blocked);
			TextView tv = (TextView) view
					.findViewById(R.id.hwa_settings_packagename);
			String packageName = (String) tv.getText();
			if (cb.isChecked()) {
				boolean disabled = disableHwa(packageName);
				if (disabled) {
					Toast.makeText(
							mContext,
							mContext.getString(
									R.string.hwa_settings_hwa_disabled_toast,
									packageName), Toast.LENGTH_SHORT).show();
					cb.setChecked(false);
				} else
					Toast.makeText(
							mContext,
							mContext.getString(
									R.string.hwa_settings_hwa_disable_failed_toast,
									packageName), Toast.LENGTH_SHORT).show();
			} else {
				boolean enabled = enableHwa(packageName);
				if (enabled) {
					Toast.makeText(
							mContext,
							mContext.getString(
									R.string.hwa_settings_hwa_enabled_toast,
									packageName), Toast.LENGTH_SHORT).show();
					cb.setChecked(true);
				} else
					Toast.makeText(
							mContext,
							mContext.getString(
									R.string.hwa_settings_hwa_enable_failed_toast,
									packageName), Toast.LENGTH_SHORT).show();
			}

		}

		private boolean enableHwa(String packageName) {
			return new File("/data/local/hwui.deny/" + packageName).delete();
		}

		private boolean disableHwa(String packageName) {
			try {
				return new File("/data/local/hwui.deny/" + packageName)
						.createNewFile();
			} catch (IOException e) {
				Log.w(TAG, "Creation of /data/local/hwui.deny/" + packageName
						+ " failed : IOException");
				return false;
			}
		}
	};
}
