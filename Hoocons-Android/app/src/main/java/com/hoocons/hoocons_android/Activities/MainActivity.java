package com.hoocons.hoocons_android.Activities;

import android.support.design.widget.TabLayout;
import android.support.v4.view.ViewPager;
import android.os.Bundle;

import com.hoocons.hoocons_android.Adapters.MainViewPagerAdapter;
import com.hoocons.hoocons_android.Managers.BaseActivity;
import com.hoocons.hoocons_android.R;
import com.hoocons.hoocons_android.ViewFragments.FeaturedFragment;

import butterknife.BindView;
import butterknife.ButterKnife;

public class MainActivity extends BaseActivity {
    @BindView(R.id.viewpager)
    ViewPager mViewPager;
    @BindView(R.id.tablayout)
    TabLayout mTabLayout;

    private MainViewPagerAdapter mMainViewPagerAdapter;

    private final int[] mTabsIcons = {
            R.drawable.ic_tab_home,
            R.drawable.ic_tab_discover,
            R.drawable.ic_tab_chat,
            R.drawable.ic_tab_notification,
            R.drawable.ic_tab_setting
    };

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        overridePendingTransition(R.anim.fix_anim, R.anim.slide_out_to_bottom);

        ButterKnife.bind(this);

        initViewPager();
        initTabBar();
    }


    /*
    * SETTING UP BOTTOM BAR
    * */
    private void initViewPager() {
        mMainViewPagerAdapter = new MainViewPagerAdapter(this.getBaseContext(), getSupportFragmentManager());

        /** the ViewPager requires a minimum of 1 as OffscreenPageLimit */
        int limit = (mMainViewPagerAdapter.getCount() > 1 ? mMainViewPagerAdapter.getCount() - 1 : 1);

        mViewPager.setAdapter(mMainViewPagerAdapter);
        mViewPager.addOnPageChangeListener(new ViewPager.OnPageChangeListener() {
            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
                invalidateFragmentMenus(position);
            }

            @Override
            public void onPageSelected(int position){
                invalidateFragmentMenus(position);

                switch (position) {
                    case 0:
                        // getSlidingMenu().setTouchModeAbove(SlidingMenu.TOUCHMODE_FULLSCREEN);
                        if (mMainViewPagerAdapter.getItem(position) == null) {
                            mMainViewPagerAdapter.addFragment(new FeaturedFragment(), "Featured");
                        }
                        break;
                    default:
                        // getSlidingMenu().setTouchModeAbove(SlidingMenu.TOUCHMODE_MARGIN);
                        break;
                }
            }

            @Override
            public void onPageScrollStateChanged(int state) {

            }
        });

        mViewPager.setOffscreenPageLimit(limit);
    }


    private void initTabBar() {
        mTabLayout.setupWithViewPager(mViewPager);
        if (mTabLayout != null) {
            mTabLayout.setupWithViewPager(mViewPager);
            for (int i = 0; i < mTabLayout.getTabCount(); i++) {
                mTabLayout.getTabAt(i).setIcon(mTabsIcons[i]);
            }
        }
    }

    /*
    * CHANGING MAIN MENU EACH TIME WE CHANGE A FRAGMENT
    * MENUS ARE SET FROM EACH MENU
    * */
    private void invalidateFragmentMenus(int position){
        for(int i = 0; i < mMainViewPagerAdapter.getCount(); i++){
            mMainViewPagerAdapter.getItem(i).setHasOptionsMenu(i == position);
        }
        invalidateOptionsMenu(); //or respectively its support method.
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    @Override
    protected void onStart() {
        super.onStart();
    }
}
