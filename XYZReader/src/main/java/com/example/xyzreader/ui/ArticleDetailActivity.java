package com.example.xyzreader.ui;

import android.app.Fragment;
import android.app.FragmentManager;
import android.app.LoaderManager;
import android.content.Loader;
import android.database.Cursor;
import android.os.Bundle;
import android.support.v13.app.FragmentStatePagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;

import com.example.xyzreader.R;
import com.example.xyzreader.R2;
import com.example.xyzreader.data.ArticleLoader;
import com.example.xyzreader.data.ItemsContract;

import butterknife.BindView;
import butterknife.ButterKnife;

/**
 * An activity represent ing a single Article detail screen, letting you swipe between articles.
 */
public class ArticleDetailActivity extends AppCompatActivity {

    private static final String LOG_TAG = ArticleDetailActivity.class.getSimpleName();
    private static final String EXTRA_CURRENT_ID = "extra_current_id";
    private Cursor mCursor;
    private long mCurrentId;

    private MyPagerAdapter mPagerAdapter;

    @BindView(R2.id.pager) ViewPager mPager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_article_detail);
        ButterKnife.bind(this);

        mPagerAdapter = new MyPagerAdapter(getFragmentManager());
        mPager.setAdapter(mPagerAdapter);

        mPager.setPageTransformer(true, new DepthPageTransformer());

        mPager.addOnPageChangeListener(new ViewPager.SimpleOnPageChangeListener() {
            @Override
            public void onPageScrollStateChanged(int state) {
                super.onPageScrollStateChanged(state);
//                Log.d(LOG_TAG, "onPageScrollStateChanged");
            }

            @Override
            public void onPageSelected(int position) {
                Log.d(LOG_TAG, "onPageSelected");
                if (mCursor != null) {
                    mCursor.moveToPosition(position);
                }
            }
        });


        if (savedInstanceState == null) {
            if (getIntent() != null && getIntent().getData() != null) {
                mCurrentId = ItemsContract.Items.getItemId(getIntent().getData());
                Log.d(LOG_TAG, mCurrentId+"");
            }
        } else {
            mCurrentId = savedInstanceState.getLong(EXTRA_CURRENT_ID);
        }

        getLoaderManager().initLoader(0, null, loaderCallbacks).forceLoad();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putLong(EXTRA_CURRENT_ID, mCurrentId);
    }

    private LoaderManager.LoaderCallbacks<Cursor> loaderCallbacks = new LoaderManager.LoaderCallbacks<Cursor>() {
        @Override
        public Loader<Cursor> onCreateLoader(int id, Bundle args) {
            Log.d(LOG_TAG, "onCreateLoader");
            return ArticleLoader.newAllArticlesInstance(getApplicationContext());
        }

        @Override
        public void onLoadFinished(Loader<Cursor> loader, Cursor cursor) {
            Log.d(LOG_TAG, "onLoadFinished");
            mCursor = cursor;
            mPagerAdapter.notifyDataSetChanged();
            // Select the start ID
            if (mCurrentId > 0) {
                cursor.moveToFirst();
                while (!cursor.isAfterLast()) {
                    if (cursor.getLong(ArticleLoader.Query._ID) == mCurrentId) {
                        final int position = cursor.getPosition();
                        mPager.setCurrentItem(position, false);
                        Log.d(LOG_TAG, "current item "+position);
                        break;
                    }
                    cursor.moveToNext();
                }
            }

        }

        @Override
        public void onLoaderReset(Loader<Cursor> loader) {
            Log.d(LOG_TAG, "onLoaderReset");
            mCursor = null;
            mPagerAdapter.notifyDataSetChanged();
        }
    };

    private class MyPagerAdapter extends FragmentStatePagerAdapter {
        public MyPagerAdapter(FragmentManager fm) {
            super(fm);
            Log.d(LOG_TAG, "MyPagerAdapter");
        }

        @Override
        public Fragment getItem(int position) {
            Log.d(LOG_TAG, "getItem");
            mCursor.moveToPosition(position);
            return ArticleDetailFragment.newInstance(mCursor.getLong(ArticleLoader.Query._ID));
        }

        @Override
        public int getCount() {
            Log.d(LOG_TAG, "getCount");
            return (mCursor != null) ? mCursor.getCount() : 0;
        }
    }
}
