package com.example.xyzreader.ui;

import android.app.Fragment;
import android.app.LoaderManager;
import android.content.Intent;
import android.content.Loader;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.support.design.widget.AppBarLayout;
import android.support.design.widget.CollapsingToolbarLayout;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.ShareCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.graphics.Palette;
import android.support.v7.widget.Toolbar;
import android.text.format.DateUtils;
import android.transition.Slide;
import android.transition.TransitionManager;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.load.resource.bitmap.GlideBitmapDrawable;
import com.bumptech.glide.load.resource.drawable.GlideDrawable;
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.target.Target;
import com.example.xyzreader.R;
import com.example.xyzreader.R2;
import com.example.xyzreader.data.ArticleLoader;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.GregorianCalendar;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

/**
 * A fragment representing a single Article detail screen. This fragment is
 * either contained in a {@link ArticleListActivity} in two-pane mode (on
 * tablets) or a {@link ArticleDetailActivity} on handsets.
 */
public class ArticleDetailFragment extends Fragment {
    private static final String TAG = "ArticleDetailFragment";

    private static final String ARG_ITEM_ID = "item_id";
    private Cursor mCursor;
    private long mItemId;

    private final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.sss");
    // Use default locale format
    private final SimpleDateFormat outputFormat = new SimpleDateFormat();
    // Most time functions can only handle 1902 - 2037
    private final GregorianCalendar START_OF_EPOCH = new GregorianCalendar(2,1,1);


    private View mRootView;

    @BindView(R2.id.app_bar_layout) AppBarLayout appBarLayout;
    @BindView(R2.id.progress_bar) ProgressBar progressBar;
    @BindView(R2.id.toolbar) Toolbar toolbar;
    @BindView(R2.id.collapsing_tool_bar) CollapsingToolbarLayout collapsingToolbarLayout;
    @BindView(R2.id.share_fab) FloatingActionButton shareBtn;
    @BindView(R2.id.article_title) TextView articleTitle;
    @BindView(R2.id.article_byline) TextView articleBy;
    @BindView(R2.id.article_body) TextView articleBody;
    @BindView(R2.id.article_thumbnail) ImageView articleThumbnail;
    @BindView(R2.id.article_title_container) LinearLayout articleTitleContainer;
    @BindView(R2.id.nav_up) FloatingActionButton navUpBtn;
    @BindView(R2.id.article_scroll_view) ScrollView articleScrollView;

    public ArticleDetailFragment() {
        /**
         * Mandatory empty constructor for the fragment manager to instantiate the
         * fragment (e.g. upon screen orientation changes).
         */
    }

    public static ArticleDetailFragment newInstance(long itemId) {
        Bundle arguments = new Bundle();
        arguments.putLong(ARG_ITEM_ID, itemId);
        ArticleDetailFragment fragment = new ArticleDetailFragment();
        fragment.setArguments(arguments);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments().containsKey(ARG_ITEM_ID)) {
            mItemId = getArguments().getLong(ARG_ITEM_ID);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        mRootView = inflater.inflate(R.layout.fragment_article_detail, container, false);
        ButterKnife.bind(this, mRootView);

        ((AppCompatActivity) getActivity()).setSupportActionBar(toolbar);
        if(((AppCompatActivity) getActivity()).getSupportActionBar() != null) {
            ((AppCompatActivity) getActivity()).getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
        collapsingToolbarLayout.setExpandedTitleTextAppearance(R.style.CollapsedAppBar);
        collapsingToolbarLayout.setTitle(" ");

        /*
        * Hide initially
        * Show only when scroll down
        * */
        navUpBtn.setVisibility(View.GONE);
        appBarLayout.addOnOffsetChangedListener(new AppBarLayout.OnOffsetChangedListener() {
            @Override
            public void onOffsetChanged(AppBarLayout appBarLayout, int verticalOffset) {
                if(Math.abs(verticalOffset)-appBarLayout.getTotalScrollRange() == 0){
                    articleTitleContainer.setVisibility(View.GONE);
                    showNavUp();
                }else {
                    articleTitleContainer.setVisibility(View.VISIBLE);
                    hideNavUp();
                }
            }
        });

        // Handle back button
        toolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                getActivity().finish();
            }
        });

        bindViews();
        return mRootView;
    }

    private void showNavUp() {
        // Add animation
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
            Slide slide = new Slide();
            slide.setSlideEdge(Gravity.TOP);

            ViewGroup viewGroup = getActivity().findViewById(android.R.id.content);
            TransitionManager.beginDelayedTransition(viewGroup, slide);
        }
        navUpBtn.setVisibility(View.VISIBLE);
    }

    private void hideNavUp() {
        // Add animation
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
            Slide slide = new Slide();
            slide.setSlideEdge(Gravity.BOTTOM);

            ViewGroup viewGroup = getActivity().findViewById(android.R.id.content);
            TransitionManager.beginDelayedTransition(viewGroup, slide);
        }
        navUpBtn.setVisibility(View.GONE);
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        // In support library r8, calling initLoader for a fragment in a FragmentPagerAdapter in
        // the fragment's onCreate may cause the same LoaderManager to be dealt to multiple
        // fragments because their mIndex is -1 (haven't been added to the activity yet). Thus,
        // we do this in onActivityCreated.
        progressBar.setVisibility(View.VISIBLE);
        getLoaderManager().initLoader(0, null, loaderCallbacks);
    }


    private Date parsePublishedDate() {
        try {
            String date = mCursor.getString(ArticleLoader.Query.PUBLISHED_DATE);
            return dateFormat.parse(date);
        } catch (ParseException ex) {
            Log.e(TAG, ex.getMessage());
            Log.i(TAG, "passing today's date");
            return new Date();
        }
    }

    private void bindViews() {

        if (mCursor != null) {
            progressBar.setVisibility(View.GONE);
            // Add title to toolbar (only visible when toolbar is collapsed)
            collapsingToolbarLayout.setTitle(mCursor.getString(ArticleLoader.Query.TITLE));

            String articleByText;
            Date publishedDate = parsePublishedDate();
            if (!publishedDate.before(START_OF_EPOCH.getTime())) {
                articleByText = DateUtils.getRelativeTimeSpanString(
                                publishedDate.getTime(),
                                System.currentTimeMillis(), DateUtils.HOUR_IN_MILLIS,
                                DateUtils.FORMAT_ABBREV_ALL).toString()
                                + " by "
                                + mCursor.getString(ArticleLoader.Query.AUTHOR);

            } else {
                // If date is before 1902, just show the string
                articleByText = outputFormat.format(publishedDate) + " by "
                        + mCursor.getString(ArticleLoader.Query.AUTHOR);

            }

            // Update article views
            articleTitle.setText(mCursor.getString(ArticleLoader.Query.TITLE));
            articleBy.setText(articleByText);
            articleBody.setText(mCursor.getString(ArticleLoader.Query.BODY));
            Glide.with(articleThumbnail.getContext())
                    .load(mCursor.getString(ArticleLoader.Query.THUMB_URL))
                    .diskCacheStrategy(DiskCacheStrategy.ALL)
                    .dontAnimate()
                    .listener(new RequestListener<String, GlideDrawable>() {
                        @Override
                        public boolean onException(Exception e, String model, Target<GlideDrawable> target, boolean isFirstResource) {
                            return false;
                        }

                        @Override
                        public boolean onResourceReady(GlideDrawable resource, String model, Target<GlideDrawable> target, boolean isFromMemoryCache, boolean isFirstResource) {
                            Bitmap bitmap = ((GlideBitmapDrawable) resource.getCurrent()).getBitmap();
                            Palette palette = Palette.from(bitmap).generate();
                            int defaultColor = 0xFF333333;
                            int color = palette.getDarkMutedColor(defaultColor);
                            articleTitleContainer.setBackgroundColor(color);
                            return false;
                        }
                    })
                    .into(articleThumbnail);

            appBarLayout.setVisibility(View.VISIBLE);
        } else {
            appBarLayout.setVisibility(View.GONE);
        }
    }


    @OnClick(R2.id.share_fab)
    public void sharePost(View view) {
        startActivity(Intent.createChooser(ShareCompat.IntentBuilder.from(getActivity())
                        .setType("text/plain")
                        .setText("Some sample text")
                        .getIntent(), getString(R.string.action_share)));
    }

    @OnClick(R2.id.nav_up)
    public void navUp(View view) {
        appBarLayout.setExpanded(true, true);
        articleScrollView.fullScroll(View.FOCUS_UP);
    }

    private final LoaderManager.LoaderCallbacks<Cursor> loaderCallbacks = new LoaderManager.LoaderCallbacks<Cursor>() {
        @Override
        public Loader<Cursor> onCreateLoader(int id, Bundle args) {
            return ArticleLoader.newInstanceForItemId(getActivity(), mItemId);
        }

        @Override
        public void onLoadFinished(Loader<Cursor> loader, Cursor cursor) {

            if (cursor == null || cursor.isClosed() || !cursor.moveToFirst()) {
                return;
            }

            mCursor = cursor;
            bindViews();
        }

        @Override
        public void onLoaderReset(Loader<Cursor> loader) {
            mCursor = null;
            bindViews();
        }
    };
}
