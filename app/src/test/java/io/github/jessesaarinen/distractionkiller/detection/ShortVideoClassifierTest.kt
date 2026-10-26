package io.github.jessesaarinen.distractionkiller.detection

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The scroll events and screens below are from the inspector on the test
 * phone (Galaxy S21, Android 15, 26.10.).
 */
class ShortVideoClassifierTest {

    private fun verdict(packageName: String, viewId: String?, dx: Int?, dy: Int?): ScrollVerdict =
        ShortVideoClassifier.classifyScroll(packageName, viewId, dx, dy).verdict

    @Test
    fun instagramReelsSwipeCounts() {
        assertEquals(ScrollVerdict.SWIPE, verdict(INSTAGRAM, "clips_viewer_view_pager", 0, 1530))
        assertEquals(ScrollVerdict.SWIPE, verdict(INSTAGRAM, "clips_viewer_view_pager", 0, -595))
    }

    @Test
    fun instagramReelsOpeningIsNotASwipe() {
        // Reels sends dy=0 when it opens, before the first swipe.
        assertEquals(ScrollVerdict.LAYOUT, verdict(INSTAGRAM, "clips_viewer_view_pager", 0, 0))
    }

    @Test
    fun instagramFeedDoesNotCount() {
        assertEquals(ScrollVerdict.OTHER, verdict(INSTAGRAM, "list", 0, 640))
        assertEquals(ScrollVerdict.OTHER, verdict(INSTAGRAM, null, 300, 0))
        // The feed loading more posts in the background while Reels is open.
        assertEquals(ScrollVerdict.LAYOUT, verdict(INSTAGRAM, "list", 0, 0))
        assertEquals(ScrollVerdict.LAYOUT, verdict(INSTAGRAM, "swipeable_tab_view_pager", 0, 0))
    }

    @Test
    fun youTubeShortsSwipeCountsWithoutDistance() {
        // YouTube's lists report dy=0 even when they move.
        assertEquals(ScrollVerdict.SWIPE, verdict(YOUTUBE, "reel_recycler", 0, 0))
    }

    @Test
    fun normalYouTubeDoesNotCount() {
        assertEquals(ScrollVerdict.OTHER, verdict(YOUTUBE, null, 0, 0))
        assertEquals(ScrollVerdict.OTHER, verdict(YOUTUBE, "results", 0, 0))
        assertEquals(ScrollVerdict.OTHER, verdict(YOUTUBE, "watch_list", 0, 0))
    }

    @Test
    fun youTubeShortsPlayerIsOnScreen() {
        assertEquals(true, ShortVideoClassifier.isPlayerOnScreen(YOUTUBE, YOUTUBE_SHORTS_SCREEN))
    }

    @Test
    fun youTubeHomeAndWatchPagesAreNotShorts() {
        assertEquals(false, ShortVideoClassifier.isPlayerOnScreen(YOUTUBE, YOUTUBE_HOME_SCREEN))
        assertEquals(false, ShortVideoClassifier.isPlayerOnScreen(YOUTUBE, YOUTUBE_WATCH_SCREEN))
    }

    @Test
    fun newFeedItemOnScreenIsASwipe() {
        // YouTube may send no scroll event for a Shorts swipe, but the next Short is on screen.
        assertTrue(ShortVideoClassifier.isNewItem("item 3", "item 4"))
        assertFalse(ShortVideoClassifier.isNewItem("item 4", "item 4"))
        // Shorts just opened, or the player left the screen: nothing to compare with.
        assertFalse(ShortVideoClassifier.isNewItem(null, "item 4"))
        assertFalse(ShortVideoClassifier.isNewItem("item 4", null))
    }

    @Test
    fun tikTokFeedSwipeCounts() {
        assertEquals(ScrollVerdict.SWIPE, verdict(TIKTOK, "viewpager", 0, 291))
        assertEquals(ScrollVerdict.SWIPE, verdict(TIKTOK, "viewpager", 0, -1853))
    }

    @Test
    fun tikTokSidewaysAndOtherListsDoNotCount() {
        assertEquals(ScrollVerdict.OTHER, verdict(TIKTOK, "viewpager", 1080, 0))
        assertEquals(ScrollVerdict.OTHER, verdict(TIKTOK, "a3q", 0, 400))
        assertEquals(ScrollVerdict.LAYOUT, verdict(TIKTOK, "viewpager", 0, 0))
    }

    @Test
    fun withoutDistanceTheViewDecides() {
        // Android 8 does not report dx and dy.
        assertEquals(ScrollVerdict.SWIPE, verdict(INSTAGRAM, "clips_viewer_view_pager", null, null))
        assertEquals(ScrollVerdict.OTHER, verdict(INSTAGRAM, "list", null, null))
    }

    @Test
    fun appsWithoutPlayerViewsAreDecidedBySwipes() {
        assertNull(ShortVideoClassifier.isPlayerOnScreen(INSTAGRAM, listOf("clips_viewer_view_pager")))
        assertNull(ShortVideoClassifier.isPlayerOnScreen(TIKTOK, listOf("viewpager")))
    }

    @Test
    fun unknownAppIsIgnored() {
        assertEquals(ScrollVerdict.IGNORED, verdict("com.example.other", "clips_viewer_view_pager", 0, 500))
        assertEquals(false, ShortVideoClassifier.isPlayerOnScreen("com.example.other", listOf("reel_recycler")))
    }

    @Test
    fun everyAppHasALabelAndAFeed() {
        assertEquals(listOf("TikTok", "Instagram", "YouTube"), AppMarkers.labels)
        assertTrue(AppMarkers.apps.all { it.swipeIds.isNotEmpty() })
    }

    private companion object {
        const val TIKTOK = "com.zhiliaoapp.musically"
        const val INSTAGRAM = "com.instagram.android"
        const val YOUTUBE = "com.google.android.youtube"

        val YOUTUBE_SHORTS_SCREEN = (
            "accessibility_layer_container action_bar_root appbar_layout bottom_bar_container " +
                "browse_fragment_layout_coordinator_layout collapsing_header_container content fab_container " +
                "fragment_container_view fullscreen_mode_container global_slim_status_bar_container " +
                "global_status_bar_view image more_drawer_container navigationBarBackground " +
                "navigation_bar_divider_frame nerd_stats_container new_content_dot next_gen_watch_container_layout " +
                "pane_fragment_container pane_fragment_contents pane_fragment_footer_container pivot_bar " +
                "pivot_bar_thumbnail player_overlay reel_player_footer_container reel_player_overlay_container " +
                "reel_player_overlay_root reel_player_page_container reel_player_page_content reel_player_underlay " +
                "reel_recycler reel_scrim_shorts_while_bottom_gradient reel_scrim_shorts_while_top reel_time_bar " +
                "reel_video_interactions reel_watch_fragment_root reel_watch_player reel_watch_refresher " +
                "root_pill_container slim_status_bar_player_container text thumbnail_layout toolbar " +
                "toolbar_container watch_while_layout_coordinator_layout"
            ).split(" ")

        val YOUTUBE_HOME_SCREEN = (
            "accessibility_layer_container action_bar_root appbar_layout bottom_bar_container " +
                "browse_fragment_layout_coordinator_layout chip_cloud_chip_modern_text cinematic_image_background " +
                "collapsing_header_container content fab_container filter_bar fullscreen_mode_container " +
                "global_slim_status_bar_container image loading_layout menu_item_0 menu_item_1 menu_item_2 " +
                "menu_item_view more_drawer_container navigationBarBackground navigation_bar_divider_frame " +
                "new_content_dot next_gen_watch_container_layout next_gen_watch_layout_no_player_fragment_container " +
                "pane_fragment_background_alpha_overlay pane_fragment_background_container pane_fragment_container " +
                "pane_fragment_contents pane_fragment_footer_container pivot_bar pivot_bar_thumbnail " +
                "player_fragment_container player_overlays player_overlays_custom_views_container player_view " +
                "results root_pill_container scrim_view slim_status_bar_player_container text thumbnail_layout " +
                "toolbar toolbar_container watch_player watch_status_bar_view watch_while_layout_coordinator_layout " +
                "watch_while_time_bar_view youtube_logo"
            ).split(" ")

        val YOUTUBE_WATCH_SCREEN = (
            "accessibility_layer_container action_bar_root action_button1 action_button2 action_button3 " +
                "action_button4 action_button5 avatar bottom_bar_container channel_avatar cinematic_image_background " +
                "cinematic_scrim content engagement_panel_wrapper fullscreen_mode_container ghost_card_layout " +
                "global_slim_status_bar_container inset_controls_overlay_wrapper inset_overlay_view_layout line1 " +
                "line2 more_drawer_container navigationBarBackground navigation_bar_divider_frame " +
                "next_gen_watch_container_layout next_gen_watch_layout_no_player_fragment_container " +
                "player_fragment_container player_overlays player_overlays_custom_views_container player_view " +
                "scrim_view simple_watch_action_bar slim_status_bar_player_container sticky_header_container " +
                "subtitle thumbnail_layout title title2 title_background video_info_loading_layout " +
                "video_metadata_layout watch_cinematic_background watch_list watch_panel watch_panel_scrim " +
                "watch_player watch_status_bar_view watch_while_time_bar_view watch_while_time_bar_view_overlay " +
                "youtube_controls_overlay"
            ).split(" ")
    }
}
