/*
 * This is the source code of Telegram for Android v. 5.x.x.
 * It is licensed under GNU GPL v. 2 or later.
 * You should have received a copy of the license in this archive (see LICENSE).
 *
 * Copyright Nikolai Kudashov, 2013-2018.
 */

package org.telegram.ui;

import static org.telegram.messenger.AndroidUtilities.dp;
import static org.telegram.messenger.AndroidUtilities.dpf2;
import static org.telegram.messenger.AndroidUtilities.ilerp;
import static org.telegram.messenger.AndroidUtilities.lerp;
import static org.telegram.messenger.ContactsController.PRIVACY_RULES_TYPE_ADDED_BY_PHONE;
import static org.telegram.messenger.LocaleController.formatPluralString;
import static org.telegram.messenger.LocaleController.formatPluralStringComma;
import static org.telegram.messenger.LocaleController.formatString;
import static org.telegram.messenger.LocaleController.getString;
import static org.telegram.ui.Stars.StarGiftSheet.replaceUnderstood;
import static org.telegram.ui.Stars.StarsIntroActivity.formatStarsAmountShort;
import static org.telegram.ui.bots.AffiliateProgramFragment.percents;

import android.Manifest;
import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.ActivityManager;
import android.app.Dialog;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ConfigurationInfo;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.database.DataSetObserver;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorFilter;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PixelFormat;
import android.graphics.Point;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.graphics.RadialGradient;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Shader;
import android.graphics.Typeface;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.media.MediaCodecInfo;
import android.media.MediaCodecList;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.SystemClock;
import android.text.SpannableString;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.TextPaint;
import android.text.TextUtils;
import android.text.style.CharacterStyle;
import android.text.style.ClickableSpan;
import android.text.style.ForegroundColorSpan;
import android.text.style.URLSpan;
import android.text.util.Linkify;
import android.util.Property;
import android.util.SparseArray;
import android.util.SparseIntArray;
import android.util.TypedValue;
import android.view.Display;
import android.view.Gravity;
import android.view.HapticFeedbackConstants;
import android.view.MotionEvent;
import android.view.VelocityTracker;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.accessibility.AccessibilityNodeInfo;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.Interpolator;
import android.webkit.CookieManager;
import android.webkit.WebStorage;
import android.webkit.WebView;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Keep;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.collection.LongSparseArray;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
import androidx.core.graphics.ColorUtils;
import androidx.core.math.MathUtils;
import androidx.core.view.NestedScrollingParent3;
import androidx.core.view.NestedScrollingParentHelper;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.interpolator.view.animation.FastOutSlowInInterpolator;
import androidx.recyclerview.widget.DefaultItemAnimator;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.LinearSmoothScrollerCustom;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager.widget.PagerAdapter;
import androidx.viewpager.widget.ViewPager;

import org.telegram.PhoneFormat.PhoneFormat;
import org.telegram.messenger.AccountInstance;
import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.ApplicationLoader;
import org.telegram.messenger.AuthTokensHelper;
import org.telegram.messenger.BillingController;
import org.telegram.messenger.BirthdayController;
import org.telegram.messenger.BuildVars;
import org.telegram.messenger.ChatObject;
import org.telegram.messenger.ChatThemeController;
import org.telegram.messenger.ContactsController;
import org.telegram.messenger.DialogObject;
import org.telegram.messenger.DocumentObject;
import org.telegram.messenger.Emoji;
import org.telegram.messenger.FileLoader;
import org.telegram.messenger.FileLog;
import org.telegram.messenger.FlagSecureReason;
import org.telegram.messenger.ImageLoader;
import org.telegram.messenger.ImageLocation;
import org.telegram.messenger.ImageReceiver;
import org.telegram.messenger.LanguageDetector;
import org.telegram.messenger.LiteMode;
import org.telegram.messenger.LocaleController;
import org.telegram.messenger.MediaController;
import org.telegram.messenger.MediaDataController;
import org.telegram.messenger.MessageObject;
import org.telegram.messenger.MessagesController;
import org.telegram.messenger.MessagesStorage;
import org.telegram.messenger.NotificationCenter;
import org.telegram.messenger.NotificationsController;
import org.telegram.messenger.R;
import org.telegram.messenger.SendMessagesHelper;
import org.telegram.messenger.SharedConfig;
import org.telegram.messenger.SvgHelper;
import org.telegram.messenger.UserConfig;
import org.telegram.messenger.UserObject;
import org.telegram.messenger.Utilities;
import org.telegram.messenger.browser.Browser;
import org.telegram.tgnet.ConnectionsManager;
import org.telegram.tgnet.SerializedData;
import org.telegram.tgnet.TLObject;
import org.telegram.tgnet.TLRPC;
import org.telegram.tgnet.tl.TL_account;
import org.telegram.tgnet.tl.TL_bots;
import org.telegram.tgnet.tl.TL_fragment;
import org.telegram.tgnet.tl.TL_stars;
import org.telegram.tgnet.tl.TL_stories;
import org.telegram.ui.ActionBar.ActionBar;
import org.telegram.ui.ActionBar.ActionBarMenu;
import org.telegram.ui.ActionBar.ActionBarMenuItem;
import org.telegram.ui.ActionBar.ActionBarMenuSubItem;
import org.telegram.ui.ActionBar.ActionBarPopupWindow;
import org.telegram.ui.ActionBar.AlertDialog;
import org.telegram.ui.ActionBar.BackDrawable;
import org.telegram.ui.ActionBar.BaseFragment;
import org.telegram.ui.ActionBar.BottomSheet;
import org.telegram.ui.ActionBar.INavigationLayout;
import org.telegram.ui.ActionBar.OKLCH;
import org.telegram.ui.ActionBar.SimpleTextView;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.ActionBar.ThemeDescription;
import org.telegram.ui.Business.OpeningHoursActivity;
import org.telegram.ui.Business.ProfileHoursCell;
import org.telegram.ui.Business.ProfileLocationCell;
import org.telegram.ui.Cells.AboutLinkCell;
import org.telegram.ui.Cells.AnimatedStatusView;
import org.telegram.ui.Cells.CheckBoxCell;
import org.telegram.ui.Cells.DividerCell;
import org.telegram.ui.Cells.GraySectionCell;
import org.telegram.ui.Cells.HeaderCell;
import org.telegram.ui.Cells.NotificationsCheckCell;
import org.telegram.ui.Cells.ProfileChannelCell;
import org.telegram.ui.Cells.SettingsSearchCell;
import org.telegram.ui.Cells.SettingsSuggestionCell;
import org.telegram.ui.Cells.ShadowSectionCell;
import org.telegram.ui.Cells.TextCell;
import org.telegram.ui.Cells.TextCheckCell;
import org.telegram.ui.Cells.TextDetailCell;
import org.telegram.ui.Cells.TextInfoPrivacyCell;
import org.telegram.ui.Cells.UserCell;
import org.telegram.ui.Components.AlertsCreator;
import org.telegram.ui.Components.AnimatedColor;
import org.telegram.ui.Components.AnimatedEmojiDrawable;
import org.telegram.ui.Components.AnimatedEmojiSpan;
import org.telegram.ui.Components.AnimatedFileDrawable;
import org.telegram.ui.Components.AnimatedFloat;
import org.telegram.ui.Components.AnimatedTextView;
import org.telegram.ui.Components.AnimationProperties;
import org.telegram.ui.Components.AudioPlayerAlert;
import org.telegram.ui.Components.AutoDeletePopupWrapper;
import org.telegram.ui.Components.AvatarDrawable;
import org.telegram.ui.Components.BackButtonMenu;
import org.telegram.ui.Components.BackupImageView;
import org.telegram.ui.Components.Bulletin;
import org.telegram.ui.Components.BulletinFactory;
import org.telegram.ui.Components.ButtonBounce;
import org.telegram.ui.Components.CanvasButton;
import org.telegram.ui.Components.ChatActivityInterface;
import org.telegram.ui.Components.ChatAvatarContainer;
import org.telegram.ui.Components.ChatNotificationsPopupWrapper;
import org.telegram.ui.Components.ColoredImageSpan;
import org.telegram.ui.Components.CombinedDrawable;
import org.telegram.ui.Components.CrossfadeDrawable;
import org.telegram.ui.Components.CubicBezierInterpolator;
import org.telegram.ui.Components.DotDividerSpan;
import org.telegram.ui.Components.EmojiPacksAlert;
import org.telegram.ui.Components.EmptyStubSpan;
import org.telegram.ui.Components.FloatingDebug.FloatingDebugController;
import org.telegram.ui.Components.Forum.ForumUtilities;
import org.telegram.ui.Components.HintView;
import org.telegram.ui.Components.HintsController;
import org.telegram.ui.Components.IdenticonDrawable;
import org.telegram.ui.Components.ImageUpdater;
import org.telegram.ui.Components.InstantCameraView;
import org.telegram.ui.Components.ItemOptions;
import org.telegram.ui.Components.JoinGroupAlert;
import org.telegram.ui.Components.LayoutHelper;
import org.telegram.ui.Components.LinkSpanDrawable;
import org.telegram.ui.Components.MediaActivity;
import org.telegram.ui.Components.MessagePrivateSeenView;
import org.telegram.ui.Components.ProfileActionsView;
import org.telegram.ui.Components.ProfileGalleryBlurView;
import org.telegram.ui.Components.Paint.PersistColorPalette;
import org.telegram.ui.Components.Premium.LimitPreviewView;
import org.telegram.ui.Components.Premium.PremiumFeatureBottomSheet;
import org.telegram.ui.Components.Premium.PremiumGradient;
import org.telegram.ui.Components.Premium.PremiumPreviewBottomSheet;
import org.telegram.ui.Components.Premium.ProfilePremiumCell;
import org.telegram.ui.Components.Premium.boosts.UserSelectorBottomSheet;
import org.telegram.ui.Components.ProfileGalleryView;
import org.telegram.ui.Components.ProfileGooeyView;
import org.telegram.ui.Components.ProfileMusicView;
import org.telegram.ui.Components.RLottieDrawable;
import org.telegram.ui.Components.RLottieImageView;
import org.telegram.ui.Components.RadialProgressView;
import org.telegram.ui.Components.Reactions.ReactionsLayoutInBubble;
import org.telegram.ui.Components.RecyclerListView;
import org.telegram.ui.Components.ScaleStateListAnimator;
import org.telegram.ui.Components.ScamDrawable;
import org.telegram.ui.Components.ShareAlert;
import org.telegram.ui.Components.SharedMediaLayout;
import org.telegram.ui.Components.SizeNotifierFrameLayout;
import org.telegram.ui.Components.StarRatingView;
import org.telegram.ui.Components.StickerEmptyView;
import org.telegram.ui.Components.TagEditCell;
import org.telegram.ui.Components.TimerDrawable;
import org.telegram.ui.Components.TranslateAlert2;
import org.telegram.ui.Components.TranslateAlert3;
import org.telegram.ui.Components.TypefaceSpan;
import org.telegram.ui.Components.UItem;
import org.telegram.ui.Components.UndoView;
import org.telegram.ui.Components.VectorAvatarThumbDrawable;
import org.telegram.ui.Components.blur3.BlurredBackgroundDrawableViewFactory;
import org.telegram.ui.Components.blur3.DownscaleScrollableNoiseSuppressor;
import org.telegram.ui.Components.blur3.ViewGroupPartRenderer;
import org.telegram.ui.Components.blur3.capture.IBlur3Capture;
import org.telegram.ui.Components.blur3.drawable.BlurredBackgroundDrawable;
import org.telegram.ui.Components.blur3.drawable.color.BlurredBackgroundColorProviderThemed;
import org.telegram.ui.Components.blur3.source.BlurredBackgroundSourceColor;
import org.telegram.ui.Components.blur3.source.BlurredBackgroundSourceRenderNode;
import org.telegram.ui.Components.chat.ViewPositionWatcher;
import org.telegram.ui.Components.voip.VoIPHelper;
import org.telegram.ui.Gifts.GiftSheet;
import org.telegram.ui.Stars.BotStarsActivity;
import org.telegram.ui.Stars.BotStarsController;
import org.telegram.ui.Stars.ProfileGiftsView;
import org.telegram.ui.Stars.StarGiftPatterns;
import org.telegram.ui.Stars.StarGiftSheet;
import org.telegram.ui.Stars.StarsController;
import org.telegram.ui.Stars.StarsIntroActivity;
import org.telegram.ui.Stories.ProfileStoriesView;
import org.telegram.ui.Stories.StoriesController;
import org.telegram.ui.Stories.StoriesListPlaceProvider;
import org.telegram.ui.Stories.StoryViewer;
import org.telegram.ui.Stories.recorder.ButtonWithCounterView;
import org.telegram.ui.Stories.recorder.DualCameraView;
import org.telegram.ui.Stories.recorder.HintView2;
import org.telegram.ui.Stories.recorder.StoryRecorder;
import org.telegram.ui.TON.TONIntroActivity;
import org.telegram.ui.bots.AffiliateProgramFragment;
import org.telegram.ui.bots.BotBiometry;
import org.telegram.ui.bots.BotDownloads;
import org.telegram.ui.bots.BotLocation;
import org.telegram.ui.bots.BotWebViewAttachedSheet;
import org.telegram.ui.bots.ChannelAffiliateProgramsFragment;
import org.telegram.ui.bots.SetupEmojiStatusSheet;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.time.LocalDate;
import java.time.Period;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicReference;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import me.vkryl.android.animator.BoolAnimator;

public class ProfileActivity extends BaseFragment implements NotificationCenter.NotificationCenterDelegate, DialogsActivity.DialogsActivityDelegate, SharedMediaLayout.SharedMediaPreloaderDelegate, ImageUpdater.ImageUpdaterDelegate, SharedMediaLayout.Delegate, MainTabsActivity.TabFragmentDelegate {
    private final static int PHONE_OPTION_CALL = 0,
            PHONE_OPTION_COPY = 1,
            PHONE_OPTION_TELEGRAM_CALL = 2,
            PHONE_OPTION_TELEGRAM_VIDEO_CALL = 3;

    private RecyclerListView listView;
    private RecyclerListView searchListView;
    private LinearLayoutManager layoutManager;
    private ListAdapter listAdapter;
    private SearchAdapter searchAdapter;
    private SimpleTextView[] nameTextView = new SimpleTextView[2];
    private String nameTextViewRightDrawableContentDescription = null;
    private String nameTextViewRightDrawable2ContentDescription = null;
    private SimpleTextView[] onlineTextView = new SimpleTextView[4];
    private AudioPlayerAlert.ClippingTextViewSwitcher mediaCounterTextView;
    private RLottieImageView writeButton;
    private AnimatorSet writeButtonAnimation;
    private Drawable lockIconDrawable;
    private final Drawable[] verifiedDrawable = new Drawable[2];
    private final Drawable[] premiumStarDrawable = new Drawable[2];
    private Long emojiStatusGiftId;
    private final AnimatedEmojiDrawable.SwapAnimatedEmojiDrawable[] emojiStatusDrawable = new AnimatedEmojiDrawable.SwapAnimatedEmojiDrawable[2];
    private final AnimatedEmojiDrawable.SwapAnimatedEmojiDrawable[] botVerificationDrawable = new AnimatedEmojiDrawable.SwapAnimatedEmojiDrawable[2];
    private final Drawable[] verifiedCheckDrawable = new Drawable[2];
    private final CrossfadeDrawable[] verifiedCrossfadeDrawable = new CrossfadeDrawable[2];
    private final CrossfadeDrawable[] premiumCrossfadeDrawable = new CrossfadeDrawable[2];
    private ScamDrawable scamDrawable;
    private UndoView undoView;
    private OverlaysView overlaysView;
    public SharedMediaLayout sharedMediaLayout;
    private StickerEmptyView emptyView;
    private boolean sharedMediaLayoutAttached;
    private SharedMediaLayout.SharedMediaPreloader sharedMediaPreloader;
    private boolean preloadedChannelEmojiStatuses;
    private StarRatingView ratingView;

    private View blurredView;

    private RLottieDrawable cameraDrawable;
    private RLottieDrawable cellCameraDrawable;

    private HintView fwdRestrictedHint;
//    private ProfileMetaballView metaball;
    private FrameLayout avatarContainer;
    private FrameLayout avatarContainer2;
    private ProfileActionsView actionsView;
    private MessagesController.SavedMusicList savedMusicList;
    private ProfileMusicView musicView;
    private AnimatedStatusView animatedStatusView;
    private AvatarImageView avatarImage;
    private View avatarOverlay;
    private AnimatorSet avatarAnimation;
    private RadialProgressView avatarProgressView;
    private ImageView timeItem;
    private ImageView starBgItem, starFgItem;
    private TimerDrawable timerDrawable;
    private ProfileGalleryBlurView avatarsBlurView;
    private ProfileGalleryView avatarsViewPager;
    private PagerIndicatorView avatarsViewPagerIndicatorView;
    private AvatarDrawable avatarDrawable;
    private ImageUpdater imageUpdater;
    private int avatarColor;
    TimerDrawable autoDeleteItemDrawable;
    private ProfileGooeyView avatarGooey;
    private ProfileStoriesView storyView;
    public ProfileGiftsView giftsView;

    private View scrimView = null;
    private Paint scrimPaint = new Paint(Paint.ANTI_ALIAS_FLAG) {
        @Override
        public void setAlpha(int a) {
            super.setAlpha(a);
            fragmentView.invalidate();
        }
    };
    private Paint actionBarBackgroundPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private ActionBarPopupWindow scrimPopupWindow;
    private Theme.ResourcesProvider resourcesProvider;

    private int overlayCountVisible;

    private ImageLocation prevLoadedImageLocation;

    private int lastMeasuredContentWidth;
    private int lastMeasuredContentHeight;
    private int listContentHeight;
    private boolean openingAvatar;
    private boolean fragmentViewAttached;

    private boolean doNotSetForeground;
    public boolean hasMainTabs;

    private boolean[] isOnline = new boolean[1];

    private boolean isCallAvailable;
    private boolean callItemVisible;
    private boolean videoCallItemVisible;
    private boolean editItemVisible;
    private ImageView callToActionItem;

    private ActionBarMenuItem animatingItem;
    private ActionBarMenuItem callItem;
    private ActionBarMenuItem videoCallItem;
    private ActionBarMenuItem editItem;
    private ActionBarMenuItem otherItem;
    private ActionBarMenuItem searchItem;
    private ActionBarMenuSubItem editColorItem;
    private ActionBarMenuSubItem linkItem;
    private ActionBarMenuSubItem setUsernameItem;
    private ImageView ttlIconView;
    private ActionBarMenuSubItem autoDeleteItem;
    AutoDeletePopupWrapper autoDeletePopupWrapper;
    protected float headerShadowAlpha = 1.0f;
    private int actionBarBackgroundColor;
    private TopView topView;
    private long userId;
    private long chatId;
    private long topicId;
    public boolean saved;
    private long dialogId;
    private boolean creatingChat;
    private boolean userBlocked;
    private boolean reportSpam;
    private long mergeDialogId;
    private boolean expandPhoto;
    private boolean needSendMessage;
    private boolean hasVoiceChatItem;
    private boolean isTopic;
    private boolean openSimilar;
    public boolean myProfile;
    public boolean openGifts;
    public int openGiftsCollection;
    public boolean openGiftsUpgradable;
    private boolean openedGifts;
    public boolean openCommonChats;
    public int initialStoryAlbum;

    private boolean scrolling;

    private boolean canSearchMembers;

    private boolean loadingUsers;
    private LongSparseArray<TLRPC.ChatParticipant> participantsMap = new LongSparseArray<>();
    private boolean usersEndReached;

    private long banFromGroup;
    private float backwardTransitionFromExtraHeight;
    private boolean openAnimationInProgress;
    private boolean transitionAnimationInProress;
    private boolean recreateMenuAfterAnimation;
    private int playProfileAnimation;
    private boolean needTimerImage;
    private boolean needStarImage;
    private boolean allowProfileAnimation = true;
    private boolean disableProfileAnimation = false;
    private boolean justFullyExpanded = false;
    private boolean ignoreScrollOnFullExpand = false;

    private float extraHeight;
    private float initialAnimationExtraHeight;
    private float avatarAnimationProgress;

    private int searchTransitionOffset;
    private float searchTransitionProgress;
    private Animator searchViewTransition;
    private boolean searchMode;

    private FlagSecureReason flagSecure;

    private HashMap<Integer, Integer> positionToOffset = new HashMap<>();

    private float avatarX;
    private float avatarY;
    private float avatarScale;
    private float pullUpProgress;
    private float nameX;
    private float nameY;
    private float onlineX;
    private float onlineY;
    private float expandProgress;
    private float listViewVelocityY;
    private ValueAnimator expandAnimator;
    private float currentExpandAnimatorValue;
    private float currentExpanAnimatorFracture;
    private float[] expandAnimatorValues = new float[]{0f, 1f};
    private boolean isInLandscapeMode;
    private boolean allowPullingDown;
    private boolean isPulledDown;

    private final Paint whitePaint = new Paint();

    private boolean isBot;
    private BotLocation botLocation;
    private BotBiometry botBiometry;

    private TLRPC.ChatFull chatInfo;
    private TLRPC.UserFull userInfo;

    public ProfileChannelCell.ChannelMessageFetcher profileChannelMessageFetcher;
    public boolean createdBirthdayFetcher;
    public ProfileBirthdayEffect.BirthdayEffectFetcher birthdayFetcher;

    private CharSequence currentBio;

    private long selectedUser;
    private int onlineCount = -1;
    private ArrayList<Integer> sortedUsers;

    private TLRPC.EncryptedChat currentEncryptedChat;
    private TLRPC.Chat currentChat;
    private TL_bots.BotInfo botInfo;
    private TLRPC.ChannelParticipant currentChannelParticipant;
    private TL_account.TL_password currentPassword;

    private TLRPC.FileLocation avatar;
    private TLRPC.FileLocation avatarBig;
    private ImageLocation uploadingImageLocation;

    private final static int add_contact = 1;
    private final static int block_contact = 2;
    private final static int share_contact = 3;
    private final static int edit_contact = 4;
    private final static int delete_contact = 5;
    private final static int leave_group = 7;
    private final static int invite_to_group = 9;
    private final static int share = 10;
    private final static int edit_channel = 12;
    private final static int add_shortcut = 14;
    private final static int call_item = 15;
    private final static int video_call_item = 16;
    private final static int search_members = 17;
    private final static int add_member = 18;
    private final static int statistics = 19;
    private final static int start_secret_chat = 20;
    private final static int gallery_menu_save = 21;
    private final static int view_discussion = 22;
    private final static int delete_topic = 23;
    private final static int report = 24;

    private final static int edit_info = 30;
    private final static int god_mode = 99;
    private final static int logout = 31;
    private final static int search_button = 32;
    private final static int set_as_main = 33;
    private final static int edit_avatar = 34;
    private final static int delete_avatar = 35;
    private final static int add_photo = 36;
    private final static int gift_premium = 38;
    private final static int channel_stories = 39;
    private final static int edit_color = 40;
    private final static int edit_profile = 41;
    private final static int copy_link_profile = 42;
    private final static int set_username = 43;
    private final static int bot_privacy = 44;
    private final static int delete_group = 45;
    private final static int enable_no_forwards = 46;
    private final static int disable_no_forwards = 47;

    private Rect rect = new Rect();

    private TextCell setAvatarCell;

    private int rowCount;

    private int setAvatarRow;
    private int setAvatarSectionRow;
    private int channelRow;
    private int channelDividerRow;
    private int numberSectionRow;
    private int numberRow;
    public int birthdayRow;
    private int setUsernameRow;
    private int bioRow;
    private int chatIdRow;
    private int ownerRow;
    private int phoneSuggestionSectionRow;
    private int graceSuggestionRow;
    private int graceSuggestionSectionRow;
    private int phoneSuggestionRow;
    private int passwordSuggestionSectionRow;
    private int passwordSuggestionRow;
    private int settingsSectionRow;
    private int settingsSectionRow2;
    private int notificationRow;
    private int languageRow;
    private int privacyRow;
    private int dataRow;
    private int chatRow;
    private int filtersRow;
    private int liteModeRow;
    private int stickersRow;
    private int devicesRow;
    private int devicesSectionRow;
    private int helpHeaderRow;
    private int questionRow;
    private int faqRow;
    private int policyRow;
    private int helpSectionCell;
    private int debugHeaderRow;
    private int sendLogsRow;
    private int sendLastLogsRow;
    private int clearLogsRow;
    private int switchBackendRow;
    private int versionRow;
    private int emptyRow;
    private int emptyRow2;
    private int bottomPaddingRow;
    private int infoHeaderRow;
    private int infoHeaderRowEmpty;
    private int infoEndRowEmpty;
    private int phoneRow;
    private int noteRow;
    private int locationRow;
    private int userInfoRow;
    private int channelInfoRow;
    private int usernameRow;
    private int notificationsDividerRow;
    private int notificationsRow;
    private int bizHoursRow;
    private int bizLocationRow;
    private int notificationsSimpleRow;
    private int infoStartRow, infoEndRow;
    private int infoSectionRow;
    private int affiliateRow;
    private int infoAffiliateRow;
    private int sendMessageRow;
    private int reportRow;
    private int deleteReactionRow;
    private int reportReactionRow;
    private int reportDividerRow;
    private int addToContactsRow;
    private int addToGroupButtonRow;
    private int addToGroupInfoRow;
    private int premiumRow;
    private int starsRow;
    private int tonRow;
    private int businessRow;
    private int premiumGiftingRow;
    private int premiumSectionsRow;
    private int botAppRow;
    private int unofficialSecurityRiskRow;
    private int unofficialSecurityRiskDividerRow;
    private int botPermissionsHeader;
    @Keep
    private int botPermissionLocation;
    @Keep
    private int botPermissionEmojiStatus;
    private int botPermissionEmojiStatusReqId;
    @Keep
    private int botPermissionBiometry;
    private int botPermissionsDivider;

    private int settingsTimerRow;
    private int settingsKeyRow;
    private int secretSettingsSectionRow;

    private int membersHeaderRow;
    private int membersStartRow;
    private int membersEndRow;
    private int addMemberRow;
    private int subscribersRow;
    private int subscribersRequestsRow;
    private int administratorsRow;
    private int settingsRow;
    private int botStarsBalanceRow;
    private int botTonBalanceRow;
    private int channelBalanceRow;
    private int channelBalanceSectionRow;
    private int balanceDividerRow;
    private int blockedUsersRow;
    private int membersSectionRow;
    private boolean hasMusic;

    private int sharedMediaRow;

    private int unblockRow;
    private int joinRow;
    private int lastSectionRow;

    private boolean hoursExpanded;
    private boolean hoursShownMine;

    private int transitionIndex;
    private final ArrayList<TLRPC.ChatParticipant> visibleChatParticipants = new ArrayList<>();
    private final ArrayList<Integer> visibleSortedUsers = new ArrayList<>();
    private int usersForceShowingIn = 0;

    private boolean firstLayout = true;
    private boolean invalidateScroll = true;

    PinchToZoomHelper pinchToZoomHelper;

    private View transitionOnlineText;
    private int actionBarAnimationColorFrom = 0;
    private int navigationBarAnimationColorFrom = 0;
    private int reportReactionMessageId = 0;
    private long reportReactionFromDialogId = 0;

    private boolean isFragmentPhoneNumber;

    private boolean showAddToContacts;
    private String vcardPhone;
    private String vcardFirstName;
    private String vcardLastName;

    BaseFragment previousTransitionMainFragment;
    ChatActivityInterface previousTransitionFragment;

    HashSet<Integer> notificationsExceptionTopics = new HashSet<>();

    private CharacterStyle loadingSpan;

    private final Property<ProfileActivity, Float> HEADER_SHADOW = new AnimationProperties.FloatProperty<ProfileActivity>("headerShadow") {
        @Override
        public void setValue(ProfileActivity object, float value) {
            headerShadowAlpha = value;
//            topView.invalidate();
        }

        @Override
        public Float get(ProfileActivity object) {
            return headerShadowAlpha;
        }
    };

    private PhotoViewer.PhotoViewerProvider provider = new PhotoViewer.EmptyPhotoViewerProvider() {

        @Override
        public PhotoViewer.PlaceProviderObject getPlaceForPhoto(MessageObject messageObject, TLRPC.FileLocation fileLocation, int index, boolean needPreview, boolean closing) {
            if (fileLocation == null) {
                return null;
            }

            if (avatarContainer.getScaleX() > 0.96f && closing) {
                return null;
            }

            TLRPC.FileLocation photoBig = null;
            if (userId != 0) {
                TLRPC.User user = getMessagesController().getUser(userId);
                if (user != null && user.photo != null && user.photo.photo_big != null) {
                    photoBig = user.photo.photo_big;
                }
            } else if (chatId != 0) {
                TLRPC.Chat chat = getMessagesController().getChat(chatId);
                if (chat != null && chat.photo != null && chat.photo.photo_big != null) {
                    photoBig = chat.photo.photo_big;
                }
            }

            boolean matches = photoBig != null && photoBig.local_id == fileLocation.local_id && photoBig.volume_id == fileLocation.volume_id && photoBig.dc_id == fileLocation.dc_id;
            int carouselMatchIndex = -1;
            if (avatarsViewPager != null) {
                for (int i = 0, n = avatarsViewPager.getRealCount(); i < n; i++) {
                    ImageLocation loc = avatarsViewPager.getRealImageLocation(i);
                    if (loc != null && loc.location != null && loc.location.local_id == fileLocation.local_id && loc.location.volume_id == fileLocation.volume_id && loc.dc_id == fileLocation.dc_id) {
                        carouselMatchIndex = i;
                        matches = true;
                        break;
                    }
                }
            }
            if (matches) {
                BackupImageView anchorView = avatarImage;
                boolean fromCarousel = false;
                if (carouselMatchIndex >= 0 && avatarsViewPager != null && avatarsViewPager.getVisibility() == View.VISIBLE) {
                    if (carouselMatchIndex != avatarsViewPager.getRealPosition()) {
                        avatarsViewPager.setCurrentRealPosition(carouselMatchIndex, false);
                    }
                    BackupImageView carouselView = avatarsViewPager.getCurrentItemView();
                    if (carouselView != null) {
                        anchorView = carouselView;
                        fromCarousel = true;
                    }
                }
                int[] coords = new int[2];
                anchorView.getLocationInWindow(coords);
                PhotoViewer.PlaceProviderObject object = new PhotoViewer.PlaceProviderObject();
                object.viewX = coords[0];
                object.viewY = coords[1];
                object.parentView = anchorView;
                object.imageReceiver = anchorView.getImageReceiver();
                if (userId != 0) {
                    object.dialogId = userId;
                } else if (chatId != 0) {
                    object.dialogId = -chatId;
                }
                object.thumb = object.imageReceiver.getBitmapSafe();
                object.size = -1;
                object.radius = anchorView.getImageReceiver().getRoundRadius(true);
                object.scale = fromCarousel ? 1f : avatarContainer.getScaleX();
                object.canEdit = userId == getUserConfig().clientUserId;
                object.fadeIn = !fromCarousel && avatarContainer.getScaleX() > 0.96f;
                object.keepImageReceiverVisible = fromCarousel;
                return object;
            }
            return null;
        }

        @Override
        public void willHidePhotoViewer() {
            avatarImage.getImageReceiver().setVisible(true, true);
        }

        @Override
        public void openPhotoForEdit(String file, String thumb, boolean isVideo) {
            imageUpdater.openPhotoForEdit(file, thumb, 0, isVideo);
        }
    };
    private boolean fragmentOpened;
    private NestedFrameLayout contentView;
    private float titleAnimationsYDiff;
    private float customAvatarProgress;
    private float customPhotoOffset;
    private boolean hasFallbackPhoto;
    private boolean hasCustomPhoto;
    private ImageReceiver fallbackImage;
    private FrameLayout bottomButtonsContainer;
    private FrameLayout[] bottomButtonContainer;
    private SpannableStringBuilder bottomButtonPostText;
    private SpannableStringBuilder bottomButtonPostTextAlbum;
    private ButtonWithCounterView[] bottomButton;
    private Runnable applyBulletin;
    private FrameLayout bottomButton2Container;
    private ButtonWithCounterView bottomButton2;

    public static ProfileActivity of(long dialogId) {
        Bundle bundle = new Bundle();
        if (dialogId >= 0) {
            bundle.putLong("user_id", dialogId);
        } else {
            bundle.putLong("chat_id", -dialogId);
        }
        return new ProfileActivity(bundle);
    }

    public long getTopicId() {
        return topicId;
    }

    public static class AvatarImageView extends BackupImageView implements SizeNotifierFrameLayout.IViewWithInvalidateCallback {

        private boolean isPulledDown;
        private float blurSizeFraction;
        private float avatarScale;
        public boolean isMetaballWorking = false;
        public int roundRadiusCollapse = 0;
        private int roundRadiusExpand = 0;
        private int actionsSize = 0;
        private boolean blurEnabled;

        public final Path clipPath = new Path();
        private final RectF rect = new RectF();
        private final Paint placeholderPaint;
        public boolean drawAvatar = true;
        public float bounceScale = 1f;

        private float crossfadeProgress;
        private ImageReceiver animateFromImageReceiver;

        public ImageReceiver foregroundImageReceiver;
        public float foregroundAlpha;
        private ImageReceiver.BitmapHolder drawableHolder;
        public boolean drawForeground = true;
        float progressToExpand;

        ProfileGalleryView avatarsViewPager;
        public boolean hasStories;
        private float progressToInsets = 1f;

        public void setAvatarsViewPager(ProfileGalleryView avatarsViewPager) {
            this.avatarsViewPager = avatarsViewPager;
        }

        public void createBlurEffect(int actionsSize) {
            this.actionsSize = actionsSize;
            this.blurEnabled = true; // actionsSize > 0;
        }

        public AvatarImageView(Context context) {
            super(context);
            setLayerType(View.LAYER_TYPE_HARDWARE, null);
            foregroundImageReceiver = new ImageReceiver(this);
            placeholderPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
            placeholderPaint.setColor(Color.BLACK);
        }

        public void setAnimateFromImageReceiver(ImageReceiver imageReceiver) {
            this.animateFromImageReceiver = imageReceiver;
        }

        public void setCrossfadeProgress(float crossfadeProgress) {
            this.crossfadeProgress = crossfadeProgress;
            invalidate();
        }

        public static Property<AvatarImageView, Float> CROSSFADE_PROGRESS = new AnimationProperties.FloatProperty<AvatarImageView>("crossfadeProgress") {
            @Override
            public void setValue(AvatarImageView object, float value) {
                object.setCrossfadeProgress(value);
            }

            @Override
            public Float get(AvatarImageView object) {
                return object.crossfadeProgress;
            }
        };

        public void setForegroundImage(ImageLocation imageLocation, String imageFilter, Drawable thumb) {
            foregroundImageReceiver.setImage(imageLocation, imageFilter, thumb, 0, null, null, 0);
            if (drawableHolder != null) {
                drawableHolder.release();
                drawableHolder = null;
            }
        }

        public void setForegroundImageDrawable(ImageReceiver.BitmapHolder holder) {
            if (holder != null) {
                foregroundImageReceiver.setImageBitmap(holder.drawable);
            }
            if (drawableHolder != null) {
                drawableHolder.release();
                drawableHolder = null;
            }
            drawableHolder = holder;
        }

        public float getForegroundAlpha() {
            return foregroundAlpha;
        }

        public void setForegroundAlpha(float value) {
            foregroundAlpha = value;
            invalidate();
        }

        public void clearForeground() {
            AnimatedFileDrawable drawable = foregroundImageReceiver.getAnimation();
            if (drawable != null) {
                drawable.removeSecondParentView(this);
            }
            foregroundImageReceiver.clearImage();
            if (drawableHolder != null) {
                drawableHolder.release();
                drawableHolder = null;
            }
            foregroundAlpha = 0f;
            invalidate();
        }

        protected void onDetachedFromWindow() {
            super.onDetachedFromWindow();
            foregroundImageReceiver.onDetachedFromWindow();
            if (drawableHolder != null) {
                drawableHolder.release();
                drawableHolder = null;
            }
        }

        @Override
        protected void onAttachedToWindow() {
            super.onAttachedToWindow();
            foregroundImageReceiver.onAttachedToWindow();
        }

        public int getRoundRadiusForExpand() {
            if (!blurEnabled) {
                return getRoundRadius()[0];
            } else {
                return roundRadiusExpand;
            }
        }

        public void setRoundRadiusForExpand(int value) {
            if (!blurEnabled) {
                setRoundRadius(value);
            } else {
                roundRadiusExpand = value;
                setRoundRadius(value);
            }
        }

        public void setBlurRadiusProgressForExpand(float value, float avatarScale, boolean isPulledDown) {
            blurSizeFraction = value;
            this.avatarScale = avatarScale;
            this.isPulledDown = isPulledDown;
        }

        public void setRoundRadiusCollapse(int roundRadiusCollapse) {
            this.isMetaballWorking = true;
            int old = this.roundRadiusCollapse;
            this.roundRadiusCollapse = roundRadiusCollapse;
            if (old != this.roundRadiusCollapse) {
                super.invalidate();
            }
        }

        @Override
        public void setRoundRadius(int value) {
            super.setRoundRadius(value);
            foregroundImageReceiver.setRoundRadius(value);
        }

        @Override
        protected void onDraw(Canvas canvas) {
            int thisWidth = getMeasuredWidth();
            int thisHeight = getMeasuredHeight();
            boolean shouldDrawBlur = avatarsViewPager != null &&
                    avatarsViewPager.getVisibility() == View.VISIBLE &&
                    blurEnabled && blurSizeFraction > 0f;

            ProfileGalleryBlurView blurView = null;
            if (shouldDrawBlur) {
                blurView = avatarsViewPager.getBlurDrawer();
                shouldDrawBlur = blurView != null;
            }

            float inset = hasStories ? (int) AndroidUtilities.dpf2(3.5f) : 0;
            inset *= (1f - progressToExpand);
            inset *= progressToInsets * (1f - foregroundAlpha);

            ImageReceiver imageReceiver = animatedEmojiDrawable != null ? animatedEmojiDrawable.getImageReceiver() : this.imageReceiver;

            int r = /*isMetaballWorking && !hasStories ? roundRadiusCollapse : */roundRadiusExpand;
            if (r > 0) {
                clipPath.rewind();
                AndroidUtilities.rectTmp.set(inset, inset, thisWidth - inset, thisHeight - inset);
                clipPath.addRoundRect(AndroidUtilities.rectTmp, r, r, Path.Direction.CW);
                canvas.clipPath(clipPath);
            }

            canvas.save();
            canvas.scale(bounceScale, bounceScale, thisWidth / 2f, thisHeight / 2f);

            if (shouldDrawBlur) {
                if (isPulledDown) {
                    thisHeight = Math.min(thisHeight, thisWidth);
                } else {
                    thisHeight = (int) (thisHeight - AndroidUtilities.lerp(0f, actionsSize / avatarScale, blurSizeFraction));
                }
            }

            float alpha = 1.0f;
            if (animateFromImageReceiver != null) {
                alpha *= 1.0f - crossfadeProgress;
                if (crossfadeProgress > 0.0f) {
                    final float fromAlpha = crossfadeProgress;
                    final float wasImageX = animateFromImageReceiver.getImageX();
                    final float wasImageY = animateFromImageReceiver.getImageY();
                    final float wasImageW = animateFromImageReceiver.getImageWidth();
                    final float wasImageH = animateFromImageReceiver.getImageHeight();
                    final float wasAlpha = animateFromImageReceiver.getAlpha();
                    animateFromImageReceiver.setImageCoords(inset, inset, thisWidth - inset * 2f, thisHeight - inset * 2f);
                    animateFromImageReceiver.setAlpha(fromAlpha);
                    animateFromImageReceiver.draw(canvas);
                    animateFromImageReceiver.setImageCoords(wasImageX, wasImageY, wasImageW, wasImageH);
                    animateFromImageReceiver.setAlpha(wasAlpha);
                }
            }
            if (imageReceiver != null && alpha > 0 && (foregroundAlpha < 1f || !drawForeground)) {
                imageReceiver.setImageCoords(inset, inset, thisWidth - inset * 2f, thisHeight - inset * 2f);
                final float wasAlpha = imageReceiver.getAlpha();
                imageReceiver.setAlpha(wasAlpha * alpha);
                if (drawAvatar) {
                    int wasRadius = imageReceiver.getRoundRadius()[0];
                    if (shouldDrawBlur) {
                        imageReceiver.setRoundRadius(0);
                    }
                    imageReceiver.draw(canvas);
                    if (shouldDrawBlur) {
                        imageReceiver.setRoundRadius(wasRadius);
                    }
                }
                imageReceiver.setAlpha(wasAlpha);
            }
            if (foregroundAlpha > 0f && drawForeground && alpha > 0) {
                if (foregroundImageReceiver.getDrawable() != null) {
                    foregroundImageReceiver.setImageCoords(inset, inset, thisWidth - inset * 2f, thisHeight - inset * 2f);
                    foregroundImageReceiver.setAlpha(alpha * foregroundAlpha);
                    foregroundImageReceiver.draw(canvas);
                } else {
                    rect.set(0f, 0f, thisWidth, thisHeight);
                    placeholderPaint.setAlpha((int) (alpha * foregroundAlpha * 255f));
                    final int radius = foregroundImageReceiver.getRoundRadius()[0];
                    canvas.drawRoundRect(rect, radius, radius, placeholderPaint);
                }
            }

            if (shouldDrawBlur) {
                canvas.translate(inset, inset + thisHeight);
                float fraction = !isPulledDown && !blurView.isUsingRenderNode() && avatarsViewPager.getRealPosition() != 0 ? 1f : (1f - blurSizeFraction);
                blurView.draw(canvas, this, thisWidth - inset * 2f, thisHeight - inset * 2f, true, fraction, alpha);
            }

            canvas.restore();
        }

        public void setProgressToStoriesInsets(float progressToInsets) {
            if (progressToInsets == this.progressToInsets) {
                return;
            }
            this.progressToInsets = progressToInsets;
            //if (hasStories) {
            invalidate();
            //}
        }

        public void drawForeground(boolean drawForeground) {
            this.drawForeground = drawForeground;
        }

        public ChatActivityInterface getPrevFragment() {
            return null;
        }

        public void setHasStories(boolean hasStories) {
            if (this.hasStories == hasStories) {
                return;
            }
            this.hasStories = hasStories;
            invalidate();
        }

        public void setProgressToExpand(float animatedFracture) {
            if (progressToExpand == animatedFracture) {
                return;
            }
            progressToExpand = animatedFracture;
            invalidate();
        }

        private Runnable invalidateCallback = null;

        @Override
        public void listenInvalidate(Runnable callback) {
            invalidateCallback = callback;
        }

        @Override
        public void invalidate() {
            super.invalidate();
            if (avatarsViewPager != null) {
                avatarsViewPager.invalidate();
            }
            if (invalidateCallback != null) {
                invalidateCallback.run();
            }
        }

        @Override
        public void invalidate(Rect dirty) {
            super.invalidate(dirty);
            if (invalidateCallback != null) {
                invalidateCallback.run();
            }
        }

        @Override
        public void invalidate(int l, int t, int r, int b) {
            super.invalidate(l, t, r, b);
            if (invalidateCallback != null) {
                invalidateCallback.run();
            }
        }
    }

    private class TopView extends FrameLayout {

        private int currentColor;
        private Paint paint = new Paint();

        public TopView(Context context) {
            super(context);
            setWillNotDraw(false);
        }

        @Override
        public void setBackgroundColor(int color) {
            if (color != currentColor) {
                currentColor = color;
                paint.setColor(color);
                invalidate();
                if (!hasColorById) {
                    actionBarBackgroundColor = currentColor;
                }
            }
        }

        private boolean hasColorById;
        private final AnimatedFloat hasColorAnimated = new AnimatedFloat(this, 350, CubicBezierInterpolator.EASE_OUT_QUINT);
        public int color1, color2;
        private final AnimatedColor color1Animated = new AnimatedColor(this, 350, CubicBezierInterpolator.EASE_OUT_QUINT);
        private final AnimatedColor color2Animated = new AnimatedColor(this, 350, CubicBezierInterpolator.EASE_OUT_QUINT);

        private int backgroundGradientColor1, backgroundGradientColor2, backgroundGradientX;
        public float backgroundGradientRadius, backgroundGradientY;
        public RadialGradient backgroundGradient;
        public final Matrix backgroundGradientMatrix = new Matrix();
        private final Paint backgroundPaint = new Paint(Paint.ANTI_ALIAS_FLAG);

        public void setBackgroundColorId(MessagesController.PeerColor peerColor, boolean animated) {
            if (peerColor != null) {
                hasColorById = true;
                color1 = peerColor.getBgColor1(Theme.isCurrentThemeDark());
                color2 = peerColor.getBgColor2(Theme.isCurrentThemeDark());
                actionBarBackgroundColor = ColorUtils.blendARGB(color1, color2, 0.25f);
                if (peerColor.patternColor != 0) {
                    emojiColor = peerColor.patternColor;
                    btnColor = Theme.multAlpha(peerColor.patternColor, .45f);
                } else {
                    emojiColor = PeerColorActivity.adaptProfileEmojiColor(color1);
                    btnColor = Theme.multAlpha(PeerColorActivity.adaptProfileEmojiColor(color1), .15f);
                }
            } else {
                actionBarBackgroundColor = currentColor;
                hasColorById = false;
                if (AndroidUtilities.computePerceivedBrightness(getThemedColor(Theme.key_actionBarDefault)) > .8f) {
                    emojiColor = Color.WHITE; // getThemedColor(Theme.key_windowBackgroundWhiteBlueText);
                    btnColor = Color.WHITE; // Theme.multAlpha(getThemedColor(Theme.key_windowBackgroundWhiteBlueText), .75f);
                } else if (AndroidUtilities.computePerceivedBrightness(getThemedColor(Theme.key_actionBarDefault)) < .2f) {
                    emojiColor = Theme.multAlpha(Theme.adaptHSV(getThemedColor(Theme.key_actionBarDefault), +0.02f, +0.25f), .5f);
                    btnColor = Theme.multAlpha(Theme.adaptHSV(getThemedColor(Theme.key_actionBarDefault), +0.02f, +0.25f), .35f);
                } else {
                    emojiColor = PeerColorActivity.adaptProfileEmojiColor(getThemedColor(Theme.key_actionBarDefault));
                    btnColor = Theme.multAlpha(PeerColorActivity.adaptProfileEmojiColor(getThemedColor(Theme.key_actionBarDefault)), .15f);
                }
            }
            if (!animated) {
                color1Animated.set(color1, true);
                color2Animated.set(color2, true);
            }
            invalidate();
        }

        private int emojiColor;
        private int btnColor;
        private final AnimatedEmojiDrawable.SwapAnimatedEmojiDrawable emoji = new AnimatedEmojiDrawable.SwapAnimatedEmojiDrawable(this, false, dp(20), AnimatedEmojiDrawable.CACHE_TYPE_ALERT_PREVIEW_STATIC);

        @Override
        protected void onAttachedToWindow() {
            super.onAttachedToWindow();
            emoji.attach();
        }

        @Override
        protected void onDetachedFromWindow() {
            super.onDetachedFromWindow();
            emoji.detach();
        }

        public final AnimatedFloat emojiLoadedT = new AnimatedFloat(this, 0, 440, CubicBezierInterpolator.EASE_OUT_QUINT);
        public final AnimatedFloat emojiFullT = new AnimatedFloat(this, 0, 440, CubicBezierInterpolator.EASE_OUT_QUINT);

        public boolean hasEmoji;
        private boolean emojiIsCollectible;

        public void setBackgroundEmojiId(long emojiId, boolean isCollectible, boolean animated) {
            emoji.set(emojiId, animated);
            emoji.setColor(emojiColor);
            emojiIsCollectible = isCollectible;
            if (!animated) {
                emojiFullT.force(isCollectible);
            }
            hasEmoji = hasEmoji || emojiId != 0 && emojiId != -1;
            invalidate();
        }

        private boolean emojiLoaded;

        private boolean isEmojiLoaded() {
            if (emojiLoaded) {
                return true;
            }
            if (emoji != null && emoji.getDrawable() instanceof AnimatedEmojiDrawable) {
                AnimatedEmojiDrawable drawable = (AnimatedEmojiDrawable) emoji.getDrawable();
                if (drawable.getImageReceiver() != null && drawable.getImageReceiver().hasImageLoaded()) {
                    return emojiLoaded = true;
                }
            }
            return false;
        }

        private void updateBackgroundPaint() {
            final int color1 = color1Animated.set(this.color1);
            final int color2 = color2Animated.set(this.color2);
            if (actionsView != null) {
                actionsView.setActionsColor(btnColor, hasColorById);
            }

            int gradientX = getWidth() / 2;
            if (backgroundGradient == null || backgroundGradientColor1 != color1 || backgroundGradientColor2 != color2 || backgroundGradientX != gradientX) {
                backgroundGradientRadius = dp(96) * 2;
                backgroundGradientY = (actionBar.getOccupyStatusBar() ? AndroidUtilities.statusBarHeight : 0)
                        + ActionBar.getCurrentActionBarHeight()
                        - 21 * AndroidUtilities.density
                        + actionBar.getTranslationY();
                backgroundGradient = new RadialGradient(backgroundGradientX = gradientX, backgroundGradientY + backgroundGradientRadius / 2f, backgroundGradientRadius, new int[]{backgroundGradientColor2 = color2, backgroundGradientColor1 = color1}, new float[]{0, 1}, Shader.TileMode.CLAMP);
                backgroundGradient.setLocalMatrix(backgroundGradientMatrix);
                backgroundPaint.setShader(backgroundGradient);
            }
        }

        @Override
        protected void onDraw(Canvas canvas) {
            final int height = ActionBar.getCurrentActionBarHeight() + (actionBar.getOccupyStatusBar() ? AndroidUtilities.statusBarHeight : 0);
            final float v = height + extraHeight + searchTransitionOffset;

            int y1 = (int) (v * (1.0f - mediaHeaderAnimationProgress));

            if (y1 != 0) {
                if (previousTransitionFragment != null && previousTransitionFragment.getContentView() != null) {
                    blurBounds.set(0, 0, getMeasuredWidth(), y1);
                    if (previousTransitionFragment.getActionBar() != null && !previousTransitionFragment.getContentView().blurWasDrawn() && previousTransitionFragment.getActionBar().getBackground() == null) {
                        paint.setColor(Theme.getColor(Theme.key_actionBarDefault, previousTransitionFragment.getResourceProvider()));
                        canvas.drawRect(blurBounds, paint);
                    } else if (previousTransitionMainFragment != null && previousTransitionMainFragment instanceof DialogsActivity && previousTransitionMainFragment.getFragmentView() instanceof SizeNotifierFrameLayout) {
                        previousTransitionMainFragment.getActionBar().blurScrimPaint.setColor(Theme.getColor(Theme.key_actionBarDefault, previousTransitionMainFragment.getResourceProvider()));
                        ((SizeNotifierFrameLayout) previousTransitionMainFragment.getFragmentView()).drawBlurRect(canvas, getY(), blurBounds, previousTransitionMainFragment.getActionBar().blurScrimPaint, true);
                    } else {
                        previousTransitionFragment.getContentView().drawBlurRect(canvas, getY(), blurBounds, previousTransitionFragment.getActionBar().blurScrimPaint, true);
                    }
                }
                paint.setColor(currentColor);
                updateBackgroundPaint();
                final float progressToGradient = (playProfileAnimation == 0 ? 1f : avatarAnimationProgress) * hasColorAnimated.set(hasColorById);
                if (progressToGradient < 1) {
                    canvas.drawRect(0, 0, getMeasuredWidth(), y1, paint);
                }
                if (progressToGradient > 0) {
                    backgroundPaint.setAlpha((int) (0xFF * progressToGradient));
                    canvas.drawRect(0, 0, getMeasuredWidth(), y1, backgroundPaint);
                }
                if (hasEmoji) {
                    final float loadedScale = emojiLoadedT.set(isEmojiLoaded());
                    boolean shoudIgnore = openAnimationInProgress && playProfileAnimation == 2;
                    if (!shoudIgnore && loadedScale > 0 && avatarContainer != null) {
                        canvas.save();
                        canvas.clipRect(0, 0, getMeasuredWidth(), y1);
                        float atop = actionBar.getOccupyStatusBar() ? AndroidUtilities.statusBarHeight : 0;
                        float maxExpand = getHeaderOnlyExtraHeight() + atop + (actionBar.getHeight() - atop) / 2f;
                        StarGiftPatterns.drawProfileAnimatedPattern(canvas, emoji, getMeasuredWidth(), maxExpand, calculateHeaderExtraDiff(), avatarContainer, 1.0f);
                        canvas.restore();
                    }
                }
                if (previousTransitionFragment != null) {
                    ActionBar actionBar = previousTransitionFragment.getActionBar();
                    ActionBarMenu menu = actionBar.menu;
                    if (actionBar != null && menu != null) {
                        int restoreCount = canvas.save();
                        canvas.translate(actionBar.getX() + menu.getX(), actionBar.getY() + menu.getY());
                        canvas.saveLayerAlpha(0, 0, menu.getMeasuredWidth(), menu.getMeasuredHeight(), (int) (255 * (1f - avatarAnimationProgress)), Canvas.ALL_SAVE_FLAG);
                        menu.draw(canvas);
                        canvas.restoreToCount(restoreCount);
                    }
                }
            }
            if (y1 != v) {
                int color = getThemedColor(Theme.key_windowBackgroundWhite);
                paint.setColor(color);
                blurBounds.set(0, y1, getMeasuredWidth(), (int) v);
                contentView.drawBlurRect(canvas, getY(), blurBounds, paint, true);
            }
        }

        private Rect blurBounds = new Rect();
    }

    private class OverlaysView extends View implements ProfileGalleryView.Callback {

        private final int statusBarHeight = actionBar.getOccupyStatusBar() && !inBubbleMode ? AndroidUtilities.statusBarHeight : 0;

        private final Rect topOverlayRect = new Rect();
        private final Rect bottomOverlayRect = new Rect();
        private final RectF rect = new RectF();

        private final GradientDrawable topOverlayGradient;
        private final GradientDrawable bottomOverlayGradient;
        private final ValueAnimator animator;
        private final float[] animatorValues = new float[]{0f, 1f};
        private final Paint backgroundPaint;
        private final Paint barPaint;
        private final Paint selectedBarPaint;

        private final GradientDrawable[] pressedOverlayGradient = new GradientDrawable[2];
        private final boolean[] pressedOverlayVisible = new boolean[2];
        private final float[] pressedOverlayAlpha = new float[2];

        private boolean isOverlaysVisible;
        private float currentAnimationValue;
        private float alpha = 0f;
        private float[] alphas = null;
        private long lastTime;
        private float previousSelectedProgress;
        private int previousSelectedPotision = -1;
        private float currentProgress;
        private int selectedPosition;

        private float currentLoadingAnimationProgress;
        private int currentLoadingAnimationDirection = 1;

        public OverlaysView(Context context) {
            super(context);
            setVisibility(GONE);

            barPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
            barPaint.setColor(0x55ffffff);
            selectedBarPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
            selectedBarPaint.setColor(0xffffffff);

            topOverlayGradient = new GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM, new int[]{0x42000000, 0});
            topOverlayGradient.setShape(GradientDrawable.RECTANGLE);

            bottomOverlayGradient = new GradientDrawable(GradientDrawable.Orientation.BOTTOM_TOP, new int[]{0x42000000, 0});
            bottomOverlayGradient.setShape(GradientDrawable.RECTANGLE);

            for (int i = 0; i < 2; i++) {
                final GradientDrawable.Orientation orientation = i == 0 ? GradientDrawable.Orientation.LEFT_RIGHT : GradientDrawable.Orientation.RIGHT_LEFT;
                pressedOverlayGradient[i] = new GradientDrawable(orientation, new int[]{0x32000000, 0});
                pressedOverlayGradient[i].setShape(GradientDrawable.RECTANGLE);
            }

            backgroundPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
            backgroundPaint.setColor(Color.BLACK);
            backgroundPaint.setAlpha(66);
            animator = ValueAnimator.ofFloat(0f, 1f);
            animator.setDuration(250);
            animator.setInterpolator(CubicBezierInterpolator.EASE_BOTH);
            animator.addUpdateListener(anim -> {
                float value = AndroidUtilities.lerp(animatorValues, currentAnimationValue = anim.getAnimatedFraction());
                setAlphaValue(value, true);
            });
            animator.addListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    if (!isOverlaysVisible) {
                        setVisibility(GONE);
                    }
                }

                @Override
                public void onAnimationStart(Animator animation) {
                    setVisibility(VISIBLE);
                }
            });
        }

        public void saveCurrentPageProgress() {
            previousSelectedProgress = currentProgress;
            previousSelectedPotision = selectedPosition;
            currentLoadingAnimationProgress = 0.0f;
            currentLoadingAnimationDirection = 1;
        }

        public void setAlphaValue(float value, boolean self) {
            int alpha = (int) (255 * value);
            topOverlayGradient.setAlpha(alpha);
            bottomOverlayGradient.setAlpha(alpha);
            backgroundPaint.setAlpha((int) (66 * value));
            barPaint.setAlpha((int) (0x55 * value));
            selectedBarPaint.setAlpha(alpha);
            this.alpha = value;
            if (!self) {
                currentAnimationValue = value;
            }
            invalidate();
        }

        public boolean isOverlaysVisible() {
            return isOverlaysVisible;
        }

        public void setOverlaysVisible() {
            isOverlaysVisible = true;
            setVisibility(VISIBLE);
        }

        public void setOverlaysVisible(boolean overlaysVisible, float durationFactor) {
            if (overlaysVisible != isOverlaysVisible) {
                isOverlaysVisible = overlaysVisible;
                animator.cancel();
                final float value = AndroidUtilities.lerp(animatorValues, currentAnimationValue);
                if (overlaysVisible) {
                    animator.setDuration((long) ((1f - value) * 250f / durationFactor));
                } else {
                    animator.setDuration((long) (value * 250f / durationFactor));
                }
                animatorValues[0] = value;
                animatorValues[1] = overlaysVisible ? 1f : 0f;
                animator.start();
            }
        }

        @Override
        protected void onSizeChanged(int w, int h, int oldw, int oldh) {
            final int actionBarHeight = statusBarHeight + ActionBar.getCurrentActionBarHeight();
            final float k = 0.5f;
            topOverlayRect.set(0, 0, w, (int) (actionBarHeight * k));
            bottomOverlayRect.set(0, (int) (h - AndroidUtilities.dp(72f) * k), w, h);
            topOverlayGradient.setBounds(0, topOverlayRect.bottom, w, actionBarHeight + dp(16f));
            bottomOverlayGradient.setBounds(0, h - getActionsExtraHeight() - dp(72f) - dp(24f), w, bottomOverlayRect.top);
            pressedOverlayGradient[0].setBounds(0, 0, w / 5, h);
            pressedOverlayGradient[1].setBounds(w - (w / 5), 0, w, h);
        }

        @Override
        protected void onDraw(Canvas canvas) {
            for (int i = 0; i < 2; i++) {
                if (pressedOverlayAlpha[i] > 0f) {
                    pressedOverlayGradient[i].setAlpha((int) (pressedOverlayAlpha[i] * 255));
                    pressedOverlayGradient[i].draw(canvas);
                }
            }

            topOverlayGradient.draw(canvas);
            bottomOverlayGradient.draw(canvas);
            canvas.drawRect(topOverlayRect, backgroundPaint);
            canvas.drawRect(bottomOverlayRect, backgroundPaint);

            int count = avatarsViewPager.getRealCount();
            selectedPosition = avatarsViewPager.getRealPosition();

            if (alphas == null || alphas.length != count) {
                alphas = new float[count];
                Arrays.fill(alphas, 0.0f);
            }

            boolean invalidate = false;

            long newTime = SystemClock.elapsedRealtime();
            long dt = (newTime - lastTime);
            if (dt < 0 || dt > 20) {
                dt = 17;
            }
            lastTime = newTime;

            if (count > 1 && count <= 20) {
                if (overlayCountVisible == 0) {
                    alpha = 0.0f;
                    overlayCountVisible = 3;
                } else if (overlayCountVisible == 1) {
                    alpha = 0.0f;
                    overlayCountVisible = 2;
                }
                if (overlayCountVisible == 2) {
                    barPaint.setAlpha((int) (0x55 * alpha));
                    selectedBarPaint.setAlpha((int) (0xff * alpha));
                }
                int width = (getMeasuredWidth() - dp(5 * 2) - dp(2 * (count - 1))) / count;
                int y = dp(4) + (!inBubbleMode ? AndroidUtilities.statusBarHeight : 0);
                for (int a = 0; a < count; a++) {
                    int x = dp(5 + a * 2) + width * a;
                    float progress;
                    int baseAlpha = 0x55;
                    if (a == previousSelectedPotision && Math.abs(previousSelectedProgress - 1.0f) > 0.0001f) {
                        progress = previousSelectedProgress;
                        canvas.save();
                        canvas.clipRect(x + width * progress, y, x + width, y + AndroidUtilities.dp(2));
                        rect.set(x, y, x + width, y + AndroidUtilities.dp(2));
                        barPaint.setAlpha((int) (0x55 * alpha));
                        canvas.drawRoundRect(rect, AndroidUtilities.dp(1), AndroidUtilities.dp(1), barPaint);
                        baseAlpha = 0x50;
                        canvas.restore();
                        invalidate = true;
                    } else if (a == selectedPosition) {
                        if (avatarsViewPager.isCurrentItemVideo()) {
                            progress = currentProgress = avatarsViewPager.getCurrentItemProgress();
                            if (progress <= 0 && avatarsViewPager.isLoadingCurrentVideo() || currentLoadingAnimationProgress > 0.0f) {
                                currentLoadingAnimationProgress += currentLoadingAnimationDirection * dt / 500.0f;
                                if (currentLoadingAnimationProgress > 1.0f) {
                                    currentLoadingAnimationProgress = 1.0f;
                                    currentLoadingAnimationDirection *= -1;
                                } else if (currentLoadingAnimationProgress <= 0) {
                                    currentLoadingAnimationProgress = 0.0f;
                                    currentLoadingAnimationDirection *= -1;
                                }
                            }
                            rect.set(x, y, x + width, y + AndroidUtilities.dp(2));
                            barPaint.setAlpha((int) ((0x55 + 0x30 * currentLoadingAnimationProgress) * alpha));
                            canvas.drawRoundRect(rect, AndroidUtilities.dp(1), AndroidUtilities.dp(1), barPaint);
                            invalidate = true;
                            baseAlpha = 0x50;
                        } else {
                            progress = currentProgress = 1.0f;
                        }
                    } else {
                        progress = 1.0f;
                    }
                    rect.set(x, y, x + width * progress, y + AndroidUtilities.dp(2));

                    if (a != selectedPosition) {
                        if (overlayCountVisible == 3) {
                            barPaint.setAlpha((int) (AndroidUtilities.lerp(baseAlpha, 0xff, CubicBezierInterpolator.EASE_BOTH.getInterpolation(alphas[a])) * alpha));
                        }
                    } else {
                        alphas[a] = 0.75f;
                    }

                    canvas.drawRoundRect(rect, AndroidUtilities.dp(1), AndroidUtilities.dp(1), a == selectedPosition ? selectedBarPaint : barPaint);
                }

                if (overlayCountVisible == 2) {
                    if (alpha < 1.0f) {
                        alpha += dt / 180.0f;
                        if (alpha > 1.0f) {
                            alpha = 1.0f;
                        }
                        invalidate = true;
                    } else {
                        overlayCountVisible = 3;
                    }
                } else if (overlayCountVisible == 3) {
                    for (int i = 0; i < alphas.length; i++) {
                        if (i != selectedPosition && alphas[i] > 0.0f) {
                            alphas[i] -= dt / 500.0f;
                            if (alphas[i] <= 0.0f) {
                                alphas[i] = 0.0f;
                                if (i == previousSelectedPotision) {
                                    previousSelectedPotision = -1;
                                }
                            }
                            invalidate = true;
                        } else if (i == previousSelectedPotision) {
                            previousSelectedPotision = -1;
                        }
                    }
                }
            }

            for (int i = 0; i < 2; i++) {
                if (pressedOverlayVisible[i]) {
                    if (pressedOverlayAlpha[i] < 1f) {
                        pressedOverlayAlpha[i] += dt / 180.0f;
                        if (pressedOverlayAlpha[i] > 1f) {
                            pressedOverlayAlpha[i] = 1f;
                        }
                        invalidate = true;
                    }
                } else {
                    if (pressedOverlayAlpha[i] > 0f) {
                        pressedOverlayAlpha[i] -= dt / 180.0f;
                        if (pressedOverlayAlpha[i] < 0f) {
                            pressedOverlayAlpha[i] = 0f;
                        }
                        invalidate = true;
                    }
                }
            }

            if (invalidate) {
                postInvalidateOnAnimation();
            }
        }

        @Override
        public void onDown(boolean left) {
            pressedOverlayVisible[left ? 0 : 1] = true;
            postInvalidateOnAnimation();
        }

        @Override
        public void onRelease() {
            Arrays.fill(pressedOverlayVisible, false);
            postInvalidateOnAnimation();
        }

        @Override
        public void onPhotosLoaded() {
            updateProfileData(false);
        }

        @Override
        public void onVideoSet() {
            invalidate();
        }
    }

    private class NestedFrameLayout extends SizeNotifierFrameLayout implements NestedScrollingParent3 {

        private NestedScrollingParentHelper nestedScrollingParentHelper;

        public NestedFrameLayout(Context context) {
            super(context);
            nestedScrollingParentHelper = new NestedScrollingParentHelper(this);
        }

        @Override
        public void onNestedScroll(View target, int dxConsumed, int dyConsumed, int dxUnconsumed, int dyUnconsumed, int type, int[] consumed) {
            try {
                if (target == listView && sharedMediaLayoutAttached) {
                    RecyclerListView innerListView = sharedMediaLayout.getCurrentListView();
                    int top = sharedMediaLayout.getTop();
                    if (top == 0) {
                        consumed[1] = dyUnconsumed;
                        innerListView.scrollBy(0, dyUnconsumed);
                    }
                }
            } catch (Throwable e) {
                FileLog.e(e);
                AndroidUtilities.runOnUIThread(() -> {
                    try {
                        RecyclerListView innerListView = sharedMediaLayout.getCurrentListView();
                        if (innerListView != null && innerListView.getAdapter() != null) {
                            innerListView.getAdapter().notifyDataSetChanged();
                        }
                    } catch (Throwable e2) {

                    }
                });
            }
        }

        @Override
        public void onNestedScroll(View target, int dxConsumed, int dyConsumed, int dxUnconsumed, int dyUnconsumed, int type) {

        }

        @Override
        public boolean onNestedPreFling(View target, float velocityX, float velocityY) {
            return super.onNestedPreFling(target, velocityX, velocityY);
        }

        @Override
        public void onNestedPreScroll(View target, int dx, int dy, int[] consumed, int type) {
            if (target == listView && sharedMediaRow != -1 && sharedMediaLayoutAttached) {
                boolean searchVisible = actionBar.isSearchFieldVisible();
                int t = sharedMediaLayout.getTop();
                if (dy < 0) {
                    boolean scrolledInner = false;
                    if (t <= 0) {
                        RecyclerListView innerListView = sharedMediaLayout.getCurrentListView();
                        if (innerListView != null) {
                            LinearLayoutManager linearLayoutManager = (LinearLayoutManager) innerListView.getLayoutManager();
                            int pos = linearLayoutManager.findFirstVisibleItemPosition();
                            if (pos != RecyclerView.NO_POSITION) {
                                RecyclerView.ViewHolder holder = innerListView.findViewHolderForAdapterPosition(pos);
                                int top = holder != null ? holder.itemView.getTop() : -1;
                                int paddingTop = innerListView.getPaddingTop();
                                if (top != paddingTop || pos != 0) {
                                    consumed[1] = pos != 0 ? dy : Math.max(dy, (top - paddingTop));
                                    innerListView.scrollBy(0, dy);
                                    scrolledInner = true;
                                }
                            }
                        }
                    }
                    if (searchVisible) {
                        if (!scrolledInner && t < 0) {
                            consumed[1] = dy - Math.max(t, dy);
                        } else {
                            consumed[1] = dy;
                        }
                    }
                } else {
                    if (searchVisible) {
                        RecyclerListView innerListView = sharedMediaLayout.getCurrentListView();
                        consumed[1] = dy;
                        if (t > 0) {
                            consumed[1] -= dy;
                        }
                        if (innerListView != null && consumed[1] > 0) {
                            innerListView.scrollBy(0, consumed[1]);
                        }
                    }
                }
            }
        }

        @Override
        public boolean onStartNestedScroll(View child, View target, int axes, int type) {
            return sharedMediaRow != -1 && axes == ViewCompat.SCROLL_AXIS_VERTICAL;
        }

        @Override
        public void onNestedScrollAccepted(View child, View target, int axes, int type) {
            nestedScrollingParentHelper.onNestedScrollAccepted(child, target, axes);
        }

        @Override
        public void onStopNestedScroll(View target, int type) {
            nestedScrollingParentHelper.onStopNestedScroll(target);
        }

        @Override
        public void onStopNestedScroll(View child) {

        }

        @Override
        protected void drawList(Canvas blurCanvas, boolean top, ArrayList<IViewWithInvalidateCallback> views) {
            super.drawList(blurCanvas, top, views);
            blurCanvas.save();
            blurCanvas.translate(0, listView.getY());
            sharedMediaLayout.drawListForBlur(blurCanvas, views);
            blurCanvas.restore();
        }
    }

    private class PagerIndicatorView extends View {

        private final RectF indicatorRect = new RectF();

        private final TextPaint textPaint;
        private final Paint backgroundPaint;

        private final ValueAnimator animator;
        private final float[] animatorValues = new float[]{0f, 1f};

        private final PagerAdapter adapter = avatarsViewPager.getAdapter();

        private boolean isIndicatorVisible;

        public PagerIndicatorView(Context context) {
            super(context);
            setVisibility(GONE);

            textPaint = new TextPaint(Paint.ANTI_ALIAS_FLAG);
            textPaint.setColor(Color.WHITE);
            textPaint.setTypeface(Typeface.SANS_SERIF);
            textPaint.setTextAlign(Paint.Align.CENTER);
            textPaint.setTextSize(AndroidUtilities.dpf2(15f));
            backgroundPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
            backgroundPaint.setColor(0x26000000);
            animator = ValueAnimator.ofFloat(0f, 1f);
            animator.setInterpolator(CubicBezierInterpolator.EASE_BOTH);
            animator.addUpdateListener(a -> {
                final float value = AndroidUtilities.lerp(animatorValues, a.getAnimatedFraction());
                if (searchItem != null && !isPulledDown) {
                    searchItem.setScaleX(1f - value);
                    searchItem.setScaleY(1f - value);
                    searchItem.setAlpha(1f - value);
                }
                if (editItemVisible) {
                    editItem.setScaleX(1f - value);
                    editItem.setScaleY(1f - value);
                    editItem.setAlpha(1f - value);
                }
                if (callItemVisible) {
                    callItem.setScaleX(1f - value);
                    callItem.setScaleY(1f - value);
                    callItem.setAlpha(1f - value);
                }
                if (videoCallItemVisible) {
                    videoCallItem.setScaleX(1f - value);
                    videoCallItem.setScaleY(1f - value);
                    videoCallItem.setAlpha(1f - value);
                }
                setScaleX(value);
                setScaleY(value);
                setAlpha(value);
            });
            boolean expanded = expandPhoto;
            animator.addListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    if (isIndicatorVisible) {
                        if (searchItem != null) {
                            searchItem.setClickable(false);
                        }
                        if (editItemVisible) {
                            editItem.setVisibility(GONE);
                        }
                        if (callItemVisible) {
                            callItem.setVisibility(GONE);
                        }
                        if (videoCallItemVisible) {
                            videoCallItem.setVisibility(GONE);
                        }
                    } else {
                        setVisibility(GONE);
                    }
                    updateStoriesViewBounds(false);
                }

                @Override
                public void onAnimationStart(Animator animation) {
                    if (searchItem != null && !expanded) {
                        searchItem.setClickable(true);
                    }
                    if (editItemVisible) {
                        editItem.setVisibility(VISIBLE);
                    }
                    if (callItemVisible) {
                        callItem.setVisibility(VISIBLE);
                    }
                    if (videoCallItemVisible) {
                        videoCallItem.setVisibility(VISIBLE);
                    }
                    setVisibility(VISIBLE);
                    updateStoriesViewBounds(false);
                }
            });
            avatarsViewPager.addOnPageChangeListener(new ViewPager.OnPageChangeListener() {

                private int prevPage;

                @Override
                public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
                }

                @Override
                public void onPageSelected(int position) {
                    int realPosition = avatarsViewPager.getRealPosition(position);
                    invalidateIndicatorRect(prevPage != realPosition);
                    prevPage = realPosition;
                    updateAvatarItems();
                }

                @Override
                public void onPageScrollStateChanged(int state) {
                }
            });
            adapter.registerDataSetObserver(new DataSetObserver() {
                @Override
                public void onChanged() {
                    int count = avatarsViewPager.getRealCount();
                    if (overlayCountVisible == 0 && count > 1 && count <= 20 && overlaysView.isOverlaysVisible()) {
                        overlayCountVisible = 1;
                    }
                    invalidateIndicatorRect(false);
                    refreshVisibility(1f);
                    updateAvatarItems();
                }
            });
        }

        private void updateAvatarItemsInternal() {
            if (otherItem == null || avatarsViewPager == null) {
                return;
            }
            if (isPulledDown) {
                int position = avatarsViewPager.getRealPosition();
                if (position == 0) {
                    otherItem.hideSubItem(set_as_main);
                    otherItem.showSubItem(add_photo);
                } else {
                    otherItem.showSubItem(set_as_main);
                    otherItem.hideSubItem(add_photo);
                }
            }
        }

        private void updateAvatarItems() {
            if (imageUpdater == null) {
                return;
            }
            if (otherItem.isSubMenuShowing()) {
                AndroidUtilities.runOnUIThread(this::updateAvatarItemsInternal, 500);
            } else {
                updateAvatarItemsInternal();
            }
        }

        public boolean isIndicatorVisible() {
            return isIndicatorVisible;
        }

        public boolean isIndicatorFullyVisible() {
            return isIndicatorVisible && !animator.isRunning();
        }

        public void setIndicatorVisible(boolean indicatorVisible, float durationFactor) {
            if (indicatorVisible != isIndicatorVisible) {
                isIndicatorVisible = indicatorVisible;
                animator.cancel();
                final float value = AndroidUtilities.lerp(animatorValues, animator.getAnimatedFraction());
                if (durationFactor <= 0f) {
                    animator.setDuration(0);
                } else if (indicatorVisible) {
                    animator.setDuration((long) ((1f - value) * 250f / durationFactor));
                } else {
                    animator.setDuration((long) (value * 250f / durationFactor));
                }
                animatorValues[0] = value;
                animatorValues[1] = indicatorVisible ? 1f : 0f;
                animator.start();
            }
        }

        public void refreshVisibility(float durationFactor) {
            setIndicatorVisible(isPulledDown && avatarsViewPager.getRealCount() > 20, durationFactor);
        }

        @Override
        protected void onSizeChanged(int w, int h, int oldw, int oldh) {
            invalidateIndicatorRect(false);
        }

        private void invalidateIndicatorRect(boolean pageChanged) {
            if (pageChanged) {
                overlaysView.saveCurrentPageProgress();
            }
            overlaysView.invalidate();
            final float textWidth = textPaint.measureText(getCurrentTitle());
            indicatorRect.right = getMeasuredWidth() - dp(54f);
            indicatorRect.left = indicatorRect.right - (textWidth + dpf2(16f));
            indicatorRect.top = (actionBar != null && actionBar.getOccupyStatusBar() ? AndroidUtilities.statusBarHeight : 0) + dp(15f);
            indicatorRect.bottom = indicatorRect.top + dp(26);
            setPivotX(indicatorRect.centerX());
            setPivotY(indicatorRect.centerY());
            invalidate();
        }

        @Override
        protected void onDraw(Canvas canvas) {
            final float radius = AndroidUtilities.dpf2(12);
            canvas.drawRoundRect(indicatorRect, radius, radius, backgroundPaint);
            canvas.drawText(getCurrentTitle(), indicatorRect.centerX(), indicatorRect.top + AndroidUtilities.dpf2(18.5f), textPaint);
        }

        private String getCurrentTitle() {
            return adapter.getPageTitle(avatarsViewPager.getCurrentItem()).toString();
        }

        private ActionBarMenuItem getSecondaryMenuItem() {
            if (callItemVisible) {
                return callItem;
            } else if (editItemVisible) {
                return editItem;
            } else if (searchItem != null) {
                return searchItem;
            } else {
                return null;
            }
        }
    }

    public ProfileActivity(Bundle args) {
        this(args, null);
    }

    public ProfileActivity(Bundle args, SharedMediaLayout.SharedMediaPreloader preloader) {
        super(args);
        sharedMediaPreloader = preloader;

        iBlur3SourceColor = new BlurredBackgroundSourceColor();
        iBlur3SourceColor.setColor(getThemedColor(Theme.key_windowBackgroundWhite));
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            scrollableViewNoiseSuppressor = new DownscaleScrollableNoiseSuppressor();
            //iBlur3SourceGlassFrosted = new BlurredBackgroundSourceRenderNode(null);
            iBlur3SourceGlass = new BlurredBackgroundSourceRenderNode(null);
            iBlur3FactoryLiquidGlass = new BlurredBackgroundDrawableViewFactory(iBlur3SourceGlass);
            iBlur3FactoryLiquidGlass.setLiquidGlassEffectAllowed(LiteMode.isEnabled(LiteMode.FLAG_LIQUID_GLASS));
        } else {
            scrollableViewNoiseSuppressor = null;
            //iBlur3SourceGlassFrosted = null;
            iBlur3SourceGlass = null;
            iBlur3FactoryLiquidGlass = new BlurredBackgroundDrawableViewFactory(iBlur3SourceColor);
        }
    }

    @Override
    public boolean onFragmentCreate() {
        userId = arguments.getLong("user_id", 0);
        chatId = arguments.getLong("chat_id", 0);
        topicId = arguments.getLong("topic_id", 0);
        saved = arguments.getBoolean("saved", false);
        openSimilar = arguments.getBoolean("similar", false);
        isTopic = topicId != 0;
        banFromGroup = arguments.getLong("ban_chat_id", 0);
        reportReactionMessageId = arguments.getInt("report_reaction_message_id", 0);
        reportReactionFromDialogId = arguments.getLong("report_reaction_from_dialog_id", 0);
        showAddToContacts = arguments.getBoolean("show_add_to_contacts", true);
        vcardPhone = PhoneFormat.stripExceptNumbers(arguments.getString("vcard_phone"));
        vcardFirstName = arguments.getString("vcard_first_name");
        vcardLastName = arguments.getString("vcard_last_name");
        reportSpam = arguments.getBoolean("reportSpam", false);
        myProfile = arguments.getBoolean("my_profile", false);
        openGifts = arguments.getBoolean("open_gifts", false);
        openGiftsUpgradable = arguments.getBoolean("open_gifts_upgradable", false);
        openGiftsCollection = arguments.getInt("open_gifts_collection", 0);
        openCommonChats = arguments.getBoolean("open_common", false);
        initialStoryAlbum = arguments.getInt("open_story_album_id", -1);
        hasMainTabs = arguments.getBoolean("hasMainTabs", false);
        if (!expandPhoto) {
            expandPhoto = arguments.getBoolean("expandPhoto", false);
            if (expandPhoto) {
                currentExpandAnimatorValue = 1f;
                needSendMessage = true;
            }
        }
        if (userId != 0) {
            dialogId = arguments.getLong("dialog_id", 0);
            if (dialogId != 0) {
                currentEncryptedChat = getMessagesController().getEncryptedChat(DialogObject.getEncryptedChatId(dialogId));
            }
            if (flagSecure != null) {
                flagSecure.invalidate();
            }
            TLRPC.User user = getMessagesController().getUser(userId);
            if (user == null) {
                return false;
            }

            getNotificationCenter().addObserver(this, NotificationCenter.contactsDidLoad);
            getNotificationCenter().addObserver(this, NotificationCenter.newSuggestionsAvailable);
            getNotificationCenter().addObserver(this, NotificationCenter.encryptedChatCreated);
            getNotificationCenter().addObserver(this, NotificationCenter.encryptedChatUpdated);
            getNotificationCenter().addObserver(this, NotificationCenter.blockedUsersDidLoad);
            getNotificationCenter().addObserver(this, NotificationCenter.botInfoDidLoad);
            getNotificationCenter().addObserver(this, NotificationCenter.userInfoDidLoad);
            getNotificationCenter().addObserver(this, NotificationCenter.privacyRulesUpdated);
            NotificationCenter.getGlobalInstance().addObserver(this, NotificationCenter.reloadInterface);

            userBlocked = getMessagesController().blockePeers.indexOfKey(userId) >= 0;
            if (user.bot) {
                isBot = true;
                getMediaDataController().loadBotInfo(user.id, user.id, true, classGuid);
            }
            userInfo = getMessagesController().getUserFull(userId);
            getMessagesController().loadFullUser(getMessagesController().getUser(userId), classGuid, true);
            getMessagesController().loadPeerSettings(getMessagesController().getUser(userId), null);
            participantsMap = null;

            if (UserObject.isUserSelf(user)) {
                imageUpdater = new ImageUpdater(true, ImageUpdater.FOR_TYPE_USER, true);
                imageUpdater.setOpenWithFrontfaceCamera(true);
                imageUpdater.parentFragment = this;
                imageUpdater.setDelegate(this);
                getMediaDataController().checkFeaturedStickers();
                getMessagesController().loadSuggestedFilters();
                getMessagesController().loadUserInfo(getUserConfig().getCurrentUser(), true, classGuid);
            }
            actionBarAnimationColorFrom = arguments.getInt("actionBarColor", 0);
        } else if (chatId != 0) {
            currentChat = getMessagesController().getChat(chatId);
            if (currentChat == null) {
                final CountDownLatch countDownLatch = new CountDownLatch(1);
                getMessagesStorage().getStorageQueue().postRunnable(() -> {
                    currentChat = getMessagesStorage().getChat(chatId);
                    countDownLatch.countDown();
                });
                try {
                    countDownLatch.await();
                } catch (Exception e) {
                    FileLog.e(e);
                }
                if (currentChat != null) {
                    getMessagesController().putChat(currentChat, true);
                } else {
                    return false;
                }
            }
            if (flagSecure != null) {
                flagSecure.invalidate();
            }

            if (currentChat.megagroup) {
                getChannelParticipants(true);
            } else {
                participantsMap = null;
            }
            getNotificationCenter().addObserver(this, NotificationCenter.chatInfoDidLoad);
            getNotificationCenter().addObserver(this, NotificationCenter.chatOnlineCountDidLoad);
            getNotificationCenter().addObserver(this, NotificationCenter.groupCallUpdated);
            getNotificationCenter().addObserver(this, NotificationCenter.channelRightsUpdated);
            getNotificationCenter().addObserver(this, NotificationCenter.chatWasBoostedByUser);
            NotificationCenter.getGlobalInstance().addObserver(this, NotificationCenter.uploadStoryEnd);
            sortedUsers = new ArrayList<>();
            updateOnlineCount(true);
            if (chatInfo == null) {
                chatInfo = getMessagesController().getChatFull(chatId);
            }
            if (ChatObject.isChannel(currentChat)) {
                getMessagesController().loadFullChat(chatId, classGuid, true);
            } else if (chatInfo == null) {
                chatInfo = getMessagesStorage().loadChatInfo(chatId, false, null, false, false);
            }

            updateExceptions();
        } else {
            return false;
        }

        final long selfId = getUserConfig().getClientUserId();
        if ((userId == selfId || dialogId == selfId) && !myProfile) {
            myProfile = true;
            // AndroidUtilities.printStackTrace("myProfile flag debug");
        }


        if (sharedMediaPreloader != null && sharedMediaPreloader.getTopicId() != topicId) {
            sharedMediaPreloader.onDestroy(this);
            sharedMediaPreloader = null;
        }

        if (sharedMediaPreloader == null) {
            sharedMediaPreloader = new SharedMediaLayout.SharedMediaPreloader(this);
        }
        sharedMediaPreloader.addDelegate(this);

        getNotificationCenter().addObserver(this, NotificationCenter.updateInterfaces);
        getNotificationCenter().addObserver(this, NotificationCenter.didReceiveNewMessages);
        getNotificationCenter().addObserver(this, NotificationCenter.closeChats);
        getNotificationCenter().addObserver(this, NotificationCenter.closeProfileActivity);
        getNotificationCenter().addObserver(this, NotificationCenter.topicsDidLoaded);
        getNotificationCenter().addObserver(this, NotificationCenter.updateSearchSettings);
        getNotificationCenter().addObserver(this, NotificationCenter.reloadDialogPhotos);
        getNotificationCenter().addObserver(this, NotificationCenter.storiesUpdated);
        getNotificationCenter().addObserver(this, NotificationCenter.storiesReadUpdated);
        getNotificationCenter().addObserver(this, NotificationCenter.userIsPremiumBlockedUpadted);
        getNotificationCenter().addObserver(this, NotificationCenter.currentUserPremiumStatusChanged);
        getNotificationCenter().addObserver(this, NotificationCenter.starBalanceUpdated);
        getNotificationCenter().addObserver(this, NotificationCenter.botStarsUpdated);
        getNotificationCenter().addObserver(this, NotificationCenter.botStarsTransactionsLoaded);
        getNotificationCenter().addObserver(this, NotificationCenter.dialogDeleted);
        getNotificationCenter().addObserver(this, NotificationCenter.channelRecommendationsLoaded);
        getNotificationCenter().addObserver(this, NotificationCenter.starUserGiftsLoaded);
        getNotificationCenter().addObserver(this, NotificationCenter.profileMusicUpdated);
        getNotificationCenter().addObserver(this, NotificationCenter.updatedChatRanks);
        NotificationCenter.getGlobalInstance().addObserver(this, NotificationCenter.emojiLoaded);
        updateRowsIds();
        if (listAdapter != null) {
            listAdapter.notifyDataSetChanged();
        }

        if (arguments.containsKey("preload_messages")) {
            getMessagesController().ensureMessagesLoaded(userId, 0, null);
        }

        if (userId != 0) {
            TLRPC.User user = getMessagesController().getUser(userId);

            if (UserObject.isUserSelf(user)) {
                TL_account.getPassword req = new TL_account.getPassword();
                getConnectionsManager().sendRequest(req, (response, error) -> {
                    if (response instanceof TL_account.TL_password) {
                        currentPassword = (TL_account.TL_password) response;
                    }
                });
            }
        }

        Bulletin.addDelegate(this, new Bulletin.Delegate() {
            @Override
            public int getTopOffset(int tag) {
                return AndroidUtilities.statusBarHeight + ActionBar.getCurrentActionBarHeight();
            }

            @Override
            public int getBottomOffset(int tag) {
                if (bottomButtonsContainer == null) {
                    return navigationBarHeight + additionFloatingButtonOffset;
                }
                final float stories = sharedMediaLayout.getTabVisibility(SharedMediaLayout.TAB_STORIES, true);
                final float archivedStories = sharedMediaLayout.getTabVisibility(SharedMediaLayout.TAB_ARCHIVED_STORIES, false);
                return navigationBarHeight + additionFloatingButtonOffset +
                    (int) (dp(52) - bottomButtonsContainer.getTranslationY() - archivedStories * bottomButtonContainer[1].getTranslationY() - stories * bottomButtonContainer[0].getTranslationY());
            }

            @Override
            public boolean bottomOffsetAnimated() {
                return bottomButtonsContainer == null;
            }
        });

        if (userId != 0 && UserObject.isUserSelf(getMessagesController().getUser(userId)) && !myProfile) {
            getMessagesController().getContentSettings(null);
        }

        setActionsMode();

        additionNavigationBarHeight = hasMainTabs ? dp(DialogsActivity.MAIN_TABS_HEIGHT_WITH_MARGINS) : 0;
        additionFloatingButtonOffset = hasMainTabs ? dp(DialogsActivity.MAIN_TABS_HEIGHT + DialogsActivity.MAIN_TABS_MARGIN) : 0;

        return true;
    }

    private void setActionsMode() {
        if (actionsView == null) return;

        if (myProfile) {
            actionsView.mode = ProfileActionsView.MODE_MY_PROFILE;
        } else if (isTopic) {
            actionsView.mode = ProfileActionsView.MODE_TOPIC;
        } else if (isBot) {
            actionsView.mode = ProfileActionsView.MODE_BOT;
        } else if (userId != 0) {
            actionsView.mode = ProfileActionsView.MODE_USER;
        } else if (chatId != 0) {
            TLRPC.Chat chat = getMessagesController().getChat(chatId);

            if (ChatObject.isChannel(chat)) {
                if (ChatObject.isMegagroup(chat)) {
                    actionsView.mode = ProfileActionsView.MODE_GROUP;
                } else if (ChatObject.isForum(chat)) {
                    actionsView.mode = ProfileActionsView.MODE_FORUM;
                } else {
                    actionsView.mode = ProfileActionsView.MODE_CHANNEL;
                }
            } else {
                actionsView.mode = ProfileActionsView.MODE_GROUP;
            }
        }
    }

    private void updateExceptions() {
        if (!isTopic && ChatObject.isForum(currentChat)) {
            getNotificationsController().loadTopicsNotificationsExceptions(-chatId, (topics) -> {
                ArrayList<Integer> arrayList = new ArrayList<>(topics);
                for (int i = 0; i < arrayList.size(); i++) {
                    if (getMessagesController().getTopicsController().findTopic(chatId, arrayList.get(i)) == null) {
                        arrayList.remove(i);
                        i--;
                    }
                }
                notificationsExceptionTopics.clear();
                notificationsExceptionTopics.addAll(arrayList);

                updateNotifications(true);
            });
        }
    }

    @Override
    public boolean isActionBarCrossfadeEnabled() {
        return !isPulledDown;
    }

    @Override
    public void onFragmentDestroy() {
        super.onFragmentDestroy();
        if (sharedMediaLayout != null) {
            sharedMediaLayout.onDestroy();
        }
        if (sharedMediaPreloader != null) {
            sharedMediaPreloader.onDestroy(this);
        }
        if (sharedMediaPreloader != null) {
            sharedMediaPreloader.removeDelegate(this);
        }

        getNotificationCenter().removeObserver(this, NotificationCenter.updateInterfaces);
        getNotificationCenter().removeObserver(this, NotificationCenter.closeChats);
        getNotificationCenter().removeObserver(this, NotificationCenter.closeProfileActivity);
        getNotificationCenter().removeObserver(this, NotificationCenter.didReceiveNewMessages);
        getNotificationCenter().removeObserver(this, NotificationCenter.topicsDidLoaded);
        getNotificationCenter().removeObserver(this, NotificationCenter.updateSearchSettings);
        getNotificationCenter().removeObserver(this, NotificationCenter.reloadDialogPhotos);
        getNotificationCenter().removeObserver(this, NotificationCenter.storiesUpdated);
        getNotificationCenter().removeObserver(this, NotificationCenter.storiesReadUpdated);
        getNotificationCenter().removeObserver(this, NotificationCenter.userIsPremiumBlockedUpadted);
        getNotificationCenter().removeObserver(this, NotificationCenter.currentUserPremiumStatusChanged);
        getNotificationCenter().removeObserver(this, NotificationCenter.starBalanceUpdated);
        getNotificationCenter().removeObserver(this, NotificationCenter.botStarsUpdated);
        getNotificationCenter().removeObserver(this, NotificationCenter.botStarsTransactionsLoaded);
        getNotificationCenter().removeObserver(this, NotificationCenter.dialogDeleted);
        getNotificationCenter().removeObserver(this, NotificationCenter.channelRecommendationsLoaded);
        getNotificationCenter().removeObserver(this, NotificationCenter.starUserGiftsLoaded);
        getNotificationCenter().removeObserver(this, NotificationCenter.profileMusicUpdated);
        getNotificationCenter().removeObserver(this, NotificationCenter.updatedChatRanks);
        NotificationCenter.getGlobalInstance().removeObserver(this, NotificationCenter.emojiLoaded);
        if (avatarsViewPager != null) {
            avatarsViewPager.onDestroy();
        }
        if (avatarsBlurView != null) {
            avatarsBlurView.destroy();
        }
        backwardInitialValues = null;
        if (userId != 0) {
            getNotificationCenter().removeObserver(this, NotificationCenter.newSuggestionsAvailable);
            getNotificationCenter().removeObserver(this, NotificationCenter.contactsDidLoad);
            getNotificationCenter().removeObserver(this, NotificationCenter.encryptedChatCreated);
            getNotificationCenter().removeObserver(this, NotificationCenter.encryptedChatUpdated);
            getNotificationCenter().removeObserver(this, NotificationCenter.blockedUsersDidLoad);
            getNotificationCenter().removeObserver(this, NotificationCenter.botInfoDidLoad);
            getNotificationCenter().removeObserver(this, NotificationCenter.userInfoDidLoad);
            getNotificationCenter().removeObserver(this, NotificationCenter.privacyRulesUpdated);
            NotificationCenter.getGlobalInstance().removeObserver(this, NotificationCenter.reloadInterface);
            getMessagesController().cancelLoadFullUser(userId);
        } else if (chatId != 0) {
            NotificationCenter.getGlobalInstance().removeObserver(this, NotificationCenter.uploadStoryEnd);
            getNotificationCenter().removeObserver(this, NotificationCenter.chatWasBoostedByUser);
            getNotificationCenter().removeObserver(this, NotificationCenter.chatInfoDidLoad);
            getNotificationCenter().removeObserver(this, NotificationCenter.chatOnlineCountDidLoad);
            getNotificationCenter().removeObserver(this, NotificationCenter.groupCallUpdated);
            getNotificationCenter().removeObserver(this, NotificationCenter.channelRightsUpdated);
        }
        if (avatarImage != null) {
            avatarImage.setImageDrawable(null);
        }
        if (imageUpdater != null) {
            imageUpdater.clear();
        }
        if (pinchToZoomHelper != null) {
            pinchToZoomHelper.clear();
        }
        if (birthdayFetcher != null && createdBirthdayFetcher) {
            birthdayFetcher.detach(true);
            birthdayFetcher = null;
        }

        if (applyBulletin != null) {
            Runnable runnable = applyBulletin;
            applyBulletin = null;
            AndroidUtilities.runOnUIThread(runnable);
        }

        Bulletin.removeDelegate(this);
    }

    @Override
    public ActionBar createActionBar(Context context) {
        BaseFragment lastFragment = parentLayout.getLastFragment();
        if (lastFragment instanceof ChatActivity && ((ChatActivity) lastFragment).themeDelegate != null && ((ChatActivity) lastFragment).themeDelegate.getCurrentTheme() != null && !((ChatActivity) lastFragment).themeDelegate.isGiftTheme()) {
            resourcesProvider = lastFragment.getResourceProvider();
        }
        ActionBar actionBar = new ActionBar(context, resourcesProvider) {
            @Override
            public boolean onTouchEvent(MotionEvent event) {
                avatarContainer.getHitRect(rect);
                if (rect.contains((int) event.getX(), (int) event.getY())) {
                    return false;
                }
                return super.onTouchEvent(event);
            }

            @Override
            public void setItemsColor(int color, boolean isActionMode) {
                super.setItemsColor(color, isActionMode);
                if (!isActionMode && ttlIconView != null) {
                    ttlIconView.setColorFilter(new PorterDuffColorFilter(color, PorterDuff.Mode.MULTIPLY));
                }
                if (hasMainTabs && backButtonImageView != null) {
                    backButtonImageView.setColorFilter(color, PorterDuff.Mode.SRC_IN);
                }
            }

            @Override
            protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
                super.onLayout(changed, left, top, right, bottom);
                updateStoriesViewBounds(false);
            }
        };
        actionBar.setForceSkipTouches(true);
        actionBar.setBackgroundColor(Color.TRANSPARENT);
        actionBar.setItemsBackgroundColor(peerColor != null ? 0x20ffffff : getThemedColor(Theme.key_avatar_actionBarSelectorBlue), false);
        actionBar.setItemsColor(getThemedColor(Theme.key_actionBarDefaultIcon), false);
        actionBar.setItemsColor(getThemedColor(Theme.key_actionBarDefaultIcon), true);
        actionBar.setCastShadows(false);
        actionBar.setAddToContainer(false);
        actionBar.setClipContent(true);
        actionBar.setOccupyStatusBar(hasMainTabs || !AndroidUtilities.isTablet() && !inBubbleMode);

        if (hasMainTabs) {
            actionBar.setBackButtonDrawable(new BackDrawable(false));
            actionBar.backButtonImageView.setContentDescription(getString(R.string.QrCode));
            actionBar.backButtonImageView.setImageResource(R.drawable.outline_header_qr_24);
            actionBar.backButtonImageView.setColorFilter(getThemedColor(Theme.key_actionBarDefaultIcon), PorterDuff.Mode.SRC_IN);
            actionBar.backButtonImageView.setOnClickListener(v -> {
                Bundle args = new Bundle();
                args.putLong("chat_id", chatId);
                args.putLong("user_id", userId);
                presentFragment(new QrActivity(args));
            });
        } else {
            actionBar.setBackButtonDrawable(new BackDrawable(false));
            ImageView backButton = actionBar.getBackButton();
            backButton.setOnLongClickListener(e -> {
                ActionBarPopupWindow menu = BackButtonMenu.show(this, backButton, getDialogId(), getTopicId(), resourcesProvider);
                if (menu != null) {
                    menu.setOnDismissListener(() -> dimBehindView(false));
                    dimBehindView(backButton, 0.3f);
                    if (undoView != null) {
                        undoView.hide(true, 1);
                    }
                    return true;
                } else {
                    return false;
                }
            });
        }

        return actionBar;
    }

    @Override
    public void setParentLayout(INavigationLayout layout) {
        super.setParentLayout(layout);

        if (flagSecure != null) {
            flagSecure.detach();
            flagSecure = null;
        }
        if (layout != null && layout.getParentActivity() != null) {
            flagSecure = new FlagSecureReason(layout.getParentActivity().getWindow(), () -> currentEncryptedChat != null || isPeerNoForwards());
        }
    }

    @Override
    public View createView(Context context) {
        Theme.createProfileResources(context);
        Theme.createChatResources(context, false);
        BaseFragment lastFragment = parentLayout.getLastFragment();
        if (lastFragment instanceof ChatActivity && ((ChatActivity) lastFragment).themeDelegate != null && ((ChatActivity) lastFragment).themeDelegate.getCurrentTheme() != null && !((ChatActivity) lastFragment).themeDelegate.isGiftTheme()) {
            resourcesProvider = lastFragment.getResourceProvider();
        }
        searchTransitionOffset = 0;
        searchTransitionProgress = 1f;
        searchMode = false;
        hasOwnBackground = true;
        extraHeight = getHeaderExtraHeight();
        actionBar.setActionBarMenuOnItemClick(new ActionBar.ActionBarMenuOnItemClick() {
            @Override
            public void onItemClick(final int id) {
                if (getParentActivity() == null) {
                    return;
                }
                if (id == -1) {
                    if (sharedMediaLayout != null && sharedMediaLayout.scrollSlidingTextTabStrip != null && sharedMediaLayout.scrollSlidingTextTabStrip.isReordering()) {
                        stopTabsReorder();
                        return;
                    }
                    finishFragment();
                } else if (id == block_contact) {
                    onBlockContactClicked(false);
                } else if (id == add_contact) {
                    TLRPC.User user = getMessagesController().getUser(userId);
                    Bundle args = new Bundle();
                    args.putLong("user_id", user.id);
                    args.putBoolean("addContact", true);
                    openAddToContact(user, args);
                } else if (id == share_contact) {
                    Bundle args = new Bundle();
                    args.putBoolean("onlySelect", true);
                    args.putInt("dialogsType", DialogsActivity.DIALOGS_TYPE_FORWARD);
                    args.putString("selectAlertString", LocaleController.getString(R.string.SendContactToText));
                    args.putString("selectAlertStringGroup", LocaleController.getString(R.string.SendContactToGroupText));
                    DialogsActivity fragment = new DialogsActivity(args);
                    fragment.setDelegate(ProfileActivity.this);
                    presentFragment(fragment);
                } else if (id == edit_contact) {
                    Bundle args = new Bundle();
                    args.putLong("user_id", userId);
                    presentFragment(new ContactAddActivity(args, resourcesProvider));
                } else if (id == delete_contact) {
                    final TLRPC.User user = getMessagesController().getUser(userId);
                    if (user == null || getParentActivity() == null) {
                        return;
                    }
                    AlertDialog.Builder builder = new AlertDialog.Builder(getParentActivity(), resourcesProvider);
                    builder.setTitle(LocaleController.getString(R.string.DeleteContact));
                    builder.setMessage(LocaleController.getString(R.string.AreYouSureDeleteContact));
                    builder.setPositiveButton(LocaleController.getString(R.string.Delete), (dialogInterface, i) -> {
                        ArrayList<TLRPC.User> arrayList = new ArrayList<>();
                        arrayList.add(user);
                        getContactsController().deleteContact(arrayList, true);
                        if (user != null) {
                            user.contact = false;
                            updateListAnimated(false);
                        }
                    });
                    builder.setNegativeButton(LocaleController.getString(R.string.Cancel), null);
                    AlertDialog dialog = builder.create();
                    showDialog(dialog);
                    TextView button = (TextView) dialog.getButton(DialogInterface.BUTTON_POSITIVE);
                    if (button != null) {
                        button.setTextColor(getThemedColor(Theme.key_text_RedBold));
                    }
                } else if (id == leave_group) {
                    leaveChatPressed(false);
                } else if (id == delete_group) {
                    leaveChatPressed(true);
                } else if (id == enable_no_forwards) {
                    if (!getUserConfig().isPremium()) {
                        new PremiumFeatureBottomSheet(ProfileActivity.this, getContext(), currentAccount, false, PremiumPreviewFragment.PREMIUM_FEATURE_SHARING_DISABLE, false, null).show();
                        return;
                    }

                    AlertsCreator.showDisableSharingInfo(context, resourcesProvider, () -> toggleNoForwards(true));
                } else if (id == disable_no_forwards) {
                    toggleNoForwards(false);
                } else if (id == delete_topic) {
                    AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
                    builder.setTitle(LocaleController.getPluralString("DeleteTopics", 1));
                    TLRPC.TL_forumTopic topic = MessagesController.getInstance(currentAccount).getTopicsController().findTopic(chatId, topicId);
                    builder.setMessage(formatString("DeleteSelectedTopic", R.string.DeleteSelectedTopic, topic == null ? "topic" : topic.title));
                    builder.setPositiveButton(LocaleController.getString(R.string.Delete), (dialog, which) -> {
                        ArrayList<Integer> topicIds = new ArrayList<>();
                        topicIds.add((int) topicId);
                        getMessagesController().getTopicsController().deleteTopics(chatId, topicIds);
                        playProfileAnimation = 0;
                        if (parentLayout != null && parentLayout.getFragmentStack() != null) {
                            for (int i = 0; i < parentLayout.getFragmentStack().size(); ++i) {
                                BaseFragment fragment = parentLayout.getFragmentStack().get(i);
                                if (fragment instanceof ChatActivity && ((ChatActivity) fragment).getTopicId() == topicId) {
                                    fragment.removeSelfFromStack();
                                }
                            }
                        }
                        finishFragment();

                        Context context = getContext();
                        if (context != null) {
                            BulletinFactory.of(Bulletin.BulletinWindow.make(context), resourcesProvider).createSimpleBulletin(R.raw.ic_delete, LocaleController.getPluralString("TopicsDeleted", 1)).show();
                        }
                        dialog.dismiss();
                    });
                    builder.setNegativeButton(LocaleController.getString(R.string.Cancel), (dialog, which) -> dialog.dismiss());
                    AlertDialog alertDialog = builder.create();
                    alertDialog.show();
                    TextView button = (TextView) alertDialog.getButton(DialogInterface.BUTTON_POSITIVE);
                    if (button != null) {
                        button.setTextColor(Theme.getColor(Theme.key_text_RedBold));
                    }
                } else if (id == report) {
                    ReportBottomSheet.openChat(ProfileActivity.this, getDialogId());
                } else if (id == edit_channel) {
                    if (isTopic) {
                        presentFragment(TopicCreateFragment.create(chatId, topicId));
                    } else {
                        Bundle args = new Bundle();
                        if (chatId != 0) {
                            args.putLong("chat_id", chatId);
                        } else if (isBot) {
                            args.putLong("user_id", userId);
                        }
                        ChatEditActivity fragment = new ChatEditActivity(args);
                        if (chatInfo != null) {
                            fragment.setInfo(chatInfo);
                        } else {
                            fragment.setInfo(userInfo);
                        }
                        presentFragment(fragment);
                    }
                } else if (id == edit_profile) {
                    presentFragment(new UserInfoActivity());
                } else if (id == invite_to_group) {
                    final TLRPC.User user = getMessagesController().getUser(userId);
                    if (user == null) {
                        return;
                    }
                    Bundle args = new Bundle();
                    args.putBoolean("onlySelect", true);
                    args.putInt("dialogsType", DialogsActivity.DIALOGS_TYPE_ADD_USERS_TO);
                    args.putBoolean("resetDelegate", false);
                    args.putBoolean("closeFragment", false);
//                    args.putString("addToGroupAlertString", LocaleController.formatString("AddToTheGroupAlertText", R.string.AddToTheGroupAlertText, UserObject.getUserName(user), "%1$s"));
                    DialogsActivity fragment = new DialogsActivity(args);
                    fragment.setDelegate((fragment1, dids, message, param, notify, scheduleDate, scheduleRepeatPeriod, topicsFragment) -> {
                        long did = dids.get(0).dialogId;

                        TLRPC.Chat chat = MessagesController.getInstance(currentAccount).getChat(-did);
                        if (chat != null && (chat.creator || chat.admin_rights != null && chat.admin_rights.add_admins)) {
                            getMessagesController().checkIsInChat(false, chat, user, (isInChatAlready, rightsAdmin, currentRank) -> AndroidUtilities.runOnUIThread(() -> {
                                ChatRightsEditActivity editRightsActivity = new ChatRightsEditActivity(userId, -did, rightsAdmin, null, null, currentRank, ChatRightsEditActivity.TYPE_ADD_BOT, true, !isInChatAlready, null);
                                editRightsActivity.setDelegate(new ChatRightsEditActivity.ChatRightsEditActivityDelegate() {
                                    @Override
                                    public void didSetRights(int rights, TLRPC.TL_chatAdminRights rightsAdmin, TLRPC.TL_chatBannedRights rightsBanned, String rank) {
                                        disableProfileAnimation = true;
                                        fragment.removeSelfFromStack();
                                        getNotificationCenter().removeObserver(ProfileActivity.this, NotificationCenter.closeChats);
                                        getNotificationCenter().postNotificationName(NotificationCenter.closeChats);
                                    }

                                    @Override
                                    public void didChangeOwner(TLRPC.User user) {
                                    }
                                });
                                presentFragment(editRightsActivity);
                            }));
                        } else {
                            AlertDialog.Builder builder = new AlertDialog.Builder(getParentActivity(), resourcesProvider);
                            builder.setTitle(LocaleController.getString(R.string.AddBot));
                            String chatName = chat == null ? "" : chat.title;
                            builder.setMessage(AndroidUtilities.replaceTags(formatString("AddMembersAlertNamesText", R.string.AddMembersAlertNamesText, UserObject.getUserName(user), chatName)));
                            builder.setNegativeButton(LocaleController.getString(R.string.Cancel), null);
                            builder.setPositiveButton(LocaleController.getString(R.string.AddBot), (di, i) -> {
                                disableProfileAnimation = true;

                                Bundle args1 = new Bundle();
                                args1.putBoolean("scrollToTopOnResume", true);
                                args1.putLong("chat_id", -did);
                                if (!getMessagesController().checkCanOpenChat(args1, fragment1)) {
                                    return;
                                }
                                ChatActivity chatActivity = new ChatActivity(args1);
                                getNotificationCenter().removeObserver(ProfileActivity.this, NotificationCenter.closeChats);
                                getNotificationCenter().postNotificationName(NotificationCenter.closeChats);
                                getMessagesController().addUserToChat(-did, user, 0, null, chatActivity, true, null, null);
                                presentFragment(chatActivity, true);
                            });
                            showDialog(builder.create());
                        }
                        return true;
                    });
                    presentFragment(fragment);
                } else if (id == share) {
                    onShareClicked();
                } else if (id == add_shortcut) {
                    try {
                        long did;
                        if (currentEncryptedChat != null) {
                            did = DialogObject.makeEncryptedDialogId(currentEncryptedChat.id);
                        } else if (userId != 0) {
                            did = userId;
                        } else if (chatId != 0) {
                            did = -chatId;
                        } else {
                            return;
                        }
                        getMediaDataController().installShortcut(did, MediaDataController.SHORTCUT_TYPE_USER_OR_CHAT);
                    } catch (Exception e) {
                        FileLog.e(e);
                    }
                } else if (id == call_item || id == video_call_item) {
                    onCallClicked(id == video_call_item);
                } else if (id == search_members) {
                    Bundle args = new Bundle();
                    args.putLong("chat_id", chatId);
                    args.putInt("type", ChatUsersActivity.TYPE_USERS);
                    args.putBoolean("open_search", true);
                    ChatUsersActivity fragment = new ChatUsersActivity(args);
                    fragment.setInfo(chatInfo);
                    presentFragment(fragment);
                } else if (id == add_member) {
                    openAddMember();
                } else if (id == statistics) {
                    TLRPC.Chat chat = getMessagesController().getChat(chatId);
                    presentFragment(StatisticActivity.create(chat, false));
                } else if (id == view_discussion) {
                    openDiscussion();
                } else if (id == gift_premium) {
                    onGiftPermiumClicked();
                } else if (id == channel_stories) {
                    Bundle args = new Bundle();
                    args.putInt("type", MediaActivity.TYPE_ARCHIVED_CHANNEL_STORIES);
                    args.putLong("dialog_id", -chatId);
                    MediaActivity fragment = new MediaActivity(args, null);
                    fragment.setChatInfo(chatInfo);
                    presentFragment(fragment);
                } else if (id == start_secret_chat) {
                    AlertDialog.Builder builder = new AlertDialog.Builder(getParentActivity(), resourcesProvider);
                    builder.setTitle(LocaleController.getString(R.string.AreYouSureSecretChatTitle));
                    builder.setMessage(LocaleController.getString(R.string.AreYouSureSecretChat));
                    builder.setPositiveButton(LocaleController.getString(R.string.Start), (dialogInterface, i) -> {
                        if (MessagesController.getInstance(currentAccount).isFrozen()) {
                            AccountFrozenAlert.show(currentAccount);
                            return;
                        }
                        creatingChat = true;
                        getSecretChatHelper().startSecretChat(getParentActivity(), getMessagesController().getUser(userId));
                    });
                    builder.setNegativeButton(LocaleController.getString(R.string.Cancel), null);
                    showDialog(builder.create());
                } else if (id == bot_privacy) {
                    BotWebViewAttachedSheet.openPrivacy(currentAccount, userId);
                } else if (id == gallery_menu_save) {
                    if (getParentActivity() == null) {
                        return;
                    }
                    if (Build.VERSION.SDK_INT >= 23 && (Build.VERSION.SDK_INT <= 28 || BuildVars.NO_SCOPED_STORAGE) && getParentActivity().checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                        getParentActivity().requestPermissions(new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, BasePermissionsActivity.REQUEST_CODE_EXTERNAL_STORAGE);
                        return;
                    }
                    ImageLocation location = avatarsViewPager.getImageLocation(avatarsViewPager.getRealPosition());
                    if (location == null) {
                        return;
                    }
                    final boolean isVideo = location.imageType == FileLoader.IMAGE_TYPE_ANIMATION;
                    File f = FileLoader.getInstance(currentAccount).getPathToAttach(location.location, isVideo ? "mp4" : null, true);
                    if (isVideo && !f.exists()) {
                        f = new File(FileLoader.getDirectory(FileLoader.MEDIA_DIR_IMAGE), FileLoader.getAttachFileName(location.location, "mp4"));
                    }
                    if (f.exists()) {
                        MediaController.saveFile(f.toString(), getParentActivity(), 0, null, null, uri -> {
                            if (getParentActivity() == null) {
                                return;
                            }
                            BulletinFactory.createSaveToGalleryBulletin(ProfileActivity.this, isVideo, null).show();
                        });
                    }
                } else if (id == god_mode) {
                    TLRPC.PeerSettings ps = getMessagesController().getPeerSettings(userId);
                    String info = "🆔 User ID: " + userId;
                    if (ps != null) {
                        if (ps.phone_country != null && !ps.phone_country.isEmpty()) info += "\n🌍 Country: " + ps.phone_country;
                        if (ps.registration_month != null && !ps.registration_month.isEmpty()) info += "\n📅 Registration: " + ps.registration_month;
                    }
                    new androidx.appcompat.app.AlertDialog.Builder(getParentActivity())
                        .setTitle("🔍 God Mode Info")
                        .setMessage(info)
                        .setPositiveButton("OK", null)
                        .show();
                } else if (id == edit_info) {
                    presentFragment(new UserInfoActivity());
                } else if (id == edit_color) {
//                    if (!getUserConfig().isPremium()) {
//                        showDialog(new PremiumFeatureBottomSheet(ProfileActivity.this, PremiumPreviewFragment.PREMIUM_FEATURE_NAME_COLOR, true));
//                        return;
//                    }
                    presentFragment(new PeerColorActivity(0).startOnProfile().setOnApplied(ProfileActivity.this));
                } else if (id == copy_link_profile) {
                    TLRPC.User user = getMessagesController().getUser(userId);
                    AndroidUtilities.addToClipboard(getMessagesController().linkPrefix + "/" + UserObject.getPublicUsername(user));
                } else if (id == set_username) {
                    presentFragment(new ChangeUsernameActivity());
                } else if (id == logout) {
                    presentFragment(new LogoutActivity());
                } else if (id == set_as_main) {
                    int position = avatarsViewPager.getRealPosition();
                    TLRPC.Photo photo = avatarsViewPager.getPhoto(position);
                    if (photo == null) {
                        return;
                    }
                    avatarsViewPager.startMovePhotoToBegin(position);

                    TLRPC.TL_photos_updateProfilePhoto req = new TLRPC.TL_photos_updateProfilePhoto();
                    req.id = new TLRPC.TL_inputPhoto();
                    req.id.id = photo.id;
                    req.id.access_hash = photo.access_hash;
                    req.id.file_reference = photo.file_reference;
                    UserConfig userConfig = getUserConfig();
                    getConnectionsManager().sendRequest(req, (response, error) -> AndroidUtilities.runOnUIThread(() -> {
                        avatarsViewPager.finishSettingMainPhoto();
                        if (response instanceof TLRPC.TL_photos_photo) {
                            TLRPC.TL_photos_photo photos_photo = (TLRPC.TL_photos_photo) response;
                            getMessagesController().putUsers(photos_photo.users, false);
                            TLRPC.User user = getMessagesController().getUser(userConfig.clientUserId);
                            if (photos_photo.photo instanceof TLRPC.TL_photo) {
                                avatarsViewPager.replaceFirstPhoto(photo, photos_photo.photo);
                                if (user != null) {
                                    user.photo.photo_id = photos_photo.photo.id;
                                    userConfig.setCurrentUser(user);
                                    userConfig.saveConfig(true);
                                }
                            }
                        }
                    }));
                    undoView.showWithAction(userId, UndoView.ACTION_PROFILE_PHOTO_CHANGED, photo.video_sizes.isEmpty() ? null : 1);
                    TLRPC.User user = getMessagesController().getUser(userConfig.clientUserId);

                    TLRPC.PhotoSize bigSize = FileLoader.getClosestPhotoSizeWithSize(photo.sizes, 800);
                    if (user != null) {
                        TLRPC.PhotoSize smallSize = FileLoader.getClosestPhotoSizeWithSize(photo.sizes, 90);
                        user.photo.photo_id = photo.id;
                        user.photo.photo_small = smallSize.location;
                        user.photo.photo_big = bigSize.location;
                        userConfig.setCurrentUser(user);
                        userConfig.saveConfig(true);
                        NotificationCenter.getInstance(currentAccount).postNotificationName(NotificationCenter.mainUserInfoChanged);
                        updateProfileData(true);
                    }
                    avatarsViewPager.commitMoveToBegin();
                } else if (id == edit_avatar) {
                    if (MessagesController.getInstance(currentAccount).isFrozen()) {
                        AccountFrozenAlert.show(currentAccount);
                        return;
                    }
                    int position = avatarsViewPager.getRealPosition();
                    ImageLocation location = avatarsViewPager.getImageLocation(position);
                    if (location == null) {
                        return;
                    }

                    File f = FileLoader.getInstance(currentAccount).getPathToAttach(PhotoViewer.getFileLocation(location), PhotoViewer.getFileLocationExt(location), true);
                    boolean isVideo = location.imageType == FileLoader.IMAGE_TYPE_ANIMATION;
                    String thumb;
                    if (isVideo) {
                        ImageLocation imageLocation = avatarsViewPager.getRealImageLocation(position);
                        thumb = FileLoader.getInstance(currentAccount).getPathToAttach(PhotoViewer.getFileLocation(imageLocation), PhotoViewer.getFileLocationExt(imageLocation), true).getAbsolutePath();
                    } else {
                        thumb = null;
                    }
                    imageUpdater.openPhotoForEdit(f.getAbsolutePath(), thumb, 0, isVideo);
                } else if (id == delete_avatar) {
                    AlertDialog.Builder builder = new AlertDialog.Builder(getParentActivity(), resourcesProvider);
                    ImageLocation location = avatarsViewPager.getImageLocation(avatarsViewPager.getRealPosition());
                    if (location == null) {
                        return;
                    }
                    if (location.imageType == FileLoader.IMAGE_TYPE_ANIMATION) {
                        builder.setTitle(LocaleController.getString(R.string.AreYouSureDeleteVideoTitle));
                        builder.setMessage(getString(R.string.AreYouSureDeleteVideo));
                    } else {
                        builder.setTitle(LocaleController.getString(R.string.AreYouSureDeletePhotoTitle));
                        builder.setMessage(getString(R.string.AreYouSureDeletePhoto));
                    }
                    builder.setPositiveButton(LocaleController.getString(R.string.Delete), (dialogInterface, i) -> {
                        int position = avatarsViewPager.getRealPosition();
                        TLRPC.Photo photo = avatarsViewPager.getPhoto(position);
                        TLRPC.UserFull userFull = getUserInfo();
                        if (avatar != null && position == 0) {
                            imageUpdater.cancel();
                            if (avatarUploadingRequest != 0) {
                                getConnectionsManager().cancelRequest(avatarUploadingRequest, true);
                            }
                            allowPullingDown = !AndroidUtilities.isTablet() && !isInLandscapeMode && avatarImage.getImageReceiver().hasNotThumb() && !AndroidUtilities.isAccessibilityScreenReaderEnabled();
                            avatar = null;
                            avatarBig = null;
                            avatarsViewPager.scrolledByUser = true;
                            avatarsViewPager.removeUploadingImage(uploadingImageLocation);
                            avatarsViewPager.setCreateThumbFromParent(false);
                            updateProfileData(true);
                            showAvatarProgress(false, true);
                            getNotificationCenter().postNotificationName(NotificationCenter.updateInterfaces, MessagesController.UPDATE_MASK_ALL);
                            getNotificationCenter().postNotificationName(NotificationCenter.mainUserInfoChanged);
                            getUserConfig().saveConfig(true);
                            return;
                        }
                        if (hasFallbackPhoto && photo != null && userFull != null && userFull.fallback_photo != null && userFull.fallback_photo.id == photo.id) {
                            userFull.fallback_photo = null;
                            userFull.flags &= ~4194304;
                            getMessagesStorage().updateUserInfo(userFull, true);
                            updateProfileData(false);
                        }
                        if (avatarsViewPager.getRealCount() == 1) {
                            setForegroundImage(true);
                        }
                        if (photo == null || avatarsViewPager.getRealPosition() == 0) {
                            TLRPC.Photo nextPhoto = avatarsViewPager.getPhoto(1);
                            if (nextPhoto != null) {
                                getUserConfig().getCurrentUser().photo = new TLRPC.TL_userProfilePhoto();
                                TLRPC.PhotoSize smallSize = FileLoader.getClosestPhotoSizeWithSize(nextPhoto.sizes, 90);
                                TLRPC.PhotoSize bigSize = FileLoader.getClosestPhotoSizeWithSize(nextPhoto.sizes, 1000);
                                if (smallSize != null && bigSize != null) {
                                    getUserConfig().getCurrentUser().photo.photo_small = smallSize.location;
                                    getUserConfig().getCurrentUser().photo.photo_big = bigSize.location;
                                }
                            } else {
                                getUserConfig().getCurrentUser().photo = new TLRPC.TL_userProfilePhotoEmpty();
                            }
                            getMessagesController().deleteUserPhoto(null);
                        } else {
                            TLRPC.TL_inputPhoto inputPhoto = new TLRPC.TL_inputPhoto();
                            inputPhoto.id = photo.id;
                            inputPhoto.access_hash = photo.access_hash;
                            inputPhoto.file_reference = photo.file_reference;
                            if (inputPhoto.file_reference == null) {
                                inputPhoto.file_reference = new byte[0];
                            }
                            getMessagesController().deleteUserPhoto(inputPhoto);
                            getMessagesStorage().clearUserPhoto(userId, photo.id);
                        }
                        if (avatarsViewPager.removePhotoAtIndex(position) || avatarsViewPager.getRealCount() <= 0) {
                            avatarsViewPager.setVisibility(View.GONE);
                            avatarImage.setForegroundAlpha(1f);
                            avatarContainer.setVisibility(View.VISIBLE);
                            doNotSetForeground = true;
                            final View view = layoutManager.findViewByPosition(0);
                            if (view != null) {
                                listView.smoothScrollBy(0, view.getTop() - getHeaderExtraHeight(), CubicBezierInterpolator.EASE_OUT_QUINT);
                            }
                        }
                    });
                    builder.setNegativeButton(LocaleController.getString(R.string.Cancel), null);
                    AlertDialog alertDialog = builder.create();
                    showDialog(alertDialog);
                    TextView button = (TextView) alertDialog.getButton(DialogInterface.BUTTON_POSITIVE);
                    if (button != null) {
                        button.setTextColor(getThemedColor(Theme.key_text_RedBold));
                    }
                } else if (id == add_photo) {
                    onWriteButtonClick();
                }
            }
        });

        if (sharedMediaLayout != null) {
            sharedMediaLayout.onDestroy();
        }
        final long did;
        if (dialogId != 0) {
            did = dialogId;
        } else if (userId != 0) {
            did = userId;
        } else {
            did = -chatId;
        }

        fragmentView = new NestedFrameLayout(context) {

            @Override
            public boolean dispatchTouchEvent(MotionEvent ev) {
                if (pinchToZoomHelper.isInOverlayMode()) {
                    return pinchToZoomHelper.onTouchEvent(ev);
                }
                if (sharedMediaLayout != null && sharedMediaLayout.isInFastScroll() && sharedMediaLayout.isPinnedToTop()) {
                    return sharedMediaLayout.dispatchFastScrollEvent(ev);
                }
                if (sharedMediaLayout != null && sharedMediaLayout.checkPinchToZoom(ev)) {
                    return true;
                }
                return super.dispatchTouchEvent(ev);
            }

            private boolean ignoreLayout;
            private Paint grayPaint = new Paint();

            @Override
            public boolean hasOverlappingRendering() {
                return false;
            }

            private boolean wasPortrait;

            @Override
            protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
                final int actionBarHeight = ActionBar.getCurrentActionBarHeight() + (actionBar.getOccupyStatusBar() ? AndroidUtilities.statusBarHeight : 0);
                if (listView != null) {
                    LayoutParams layoutParams = (LayoutParams) listView.getLayoutParams();
                    if (layoutParams.topMargin != actionBarHeight) {
                        layoutParams.topMargin = actionBarHeight;
                    }
                }
                if (searchListView != null) {
                    LayoutParams layoutParams = (LayoutParams) searchListView.getLayoutParams();
                    if (layoutParams.topMargin != actionBarHeight) {
                        layoutParams.topMargin = actionBarHeight;
                    }
                }

                int height = MeasureSpec.getSize(heightMeasureSpec);
                super.onMeasure(widthMeasureSpec, MeasureSpec.makeMeasureSpec(height, MeasureSpec.EXACTLY));

                boolean changed = false;
                if (lastMeasuredContentWidth != getMeasuredWidth() || lastMeasuredContentHeight != getMeasuredHeight()) {
                    changed = lastMeasuredContentWidth != 0 && lastMeasuredContentWidth != getMeasuredWidth();
                    listContentHeight = 0;
                    int count = listAdapter.getItemCount();
                    lastMeasuredContentWidth = getMeasuredWidth();
                    lastMeasuredContentHeight = getMeasuredHeight();
                    int ws = MeasureSpec.makeMeasureSpec(getMeasuredWidth(), MeasureSpec.EXACTLY);
                    int hs = MeasureSpec.makeMeasureSpec(listView.getMeasuredHeight(), MeasureSpec.UNSPECIFIED);
                    positionToOffset.clear();
                    for (int i = 0; i < count; i++) {
                        int type = listAdapter.getItemViewType(i);
                        positionToOffset.put(i, listContentHeight);
                        if (type == ListAdapter.VIEW_TYPE_SHARED_MEDIA) {
                            listContentHeight += listView.getMeasuredHeight();
                        } else {
                            RecyclerView.ViewHolder holder = listAdapter.createViewHolder(null, type);
                            listAdapter.onBindViewHolder(holder, i);
                            holder.itemView.measure(ws, hs);
                            listContentHeight += holder.itemView.getMeasuredHeight();
                        }
                    }

                    if (emptyView != null) {
                        ((LayoutParams) emptyView.getLayoutParams()).topMargin = getHeaderExtraHeight() + AndroidUtilities.statusBarHeight;
                    }
                }

                if (previousTransitionFragment != null) {
                    nameTextView[0].setRightPadding(nameTextView[0].getMeasuredWidth() - previousTransitionFragment.getAvatarContainer().getTitleTextView().getMeasuredWidth());
                }

                if (!fragmentOpened && (expandPhoto || openAnimationInProgress && playProfileAnimation == 2)) {
                    ignoreLayout = true;

                    if (expandPhoto) {
                        if (searchItem != null) {
                            searchItem.setAlpha(0.0f);
                            searchItem.setEnabled(false);
                            searchItem.setVisibility(GONE);
                        }
                        nameTextView[1].setTextColor(Color.WHITE);
                        nameTextView[1].setPivotY(nameTextView[1].getMeasuredHeight());
                        nameTextView[1].setScaleX(1.38f);
                        nameTextView[1].setScaleY(1.38f);
                        if (scamDrawable != null) {
                            scamDrawable.setColor(Color.argb(179, 255, 255, 255));
                        }
                        if (lockIconDrawable != null) {
                            lockIconDrawable.setColorFilter(Color.WHITE, PorterDuff.Mode.MULTIPLY);
                        }
                        if (verifiedCrossfadeDrawable[0] != null) {
                            verifiedCrossfadeDrawable[0].setProgress(1f);
                        }
                        if (verifiedCrossfadeDrawable[1] != null) {
                            verifiedCrossfadeDrawable[1].setProgress(1f);
                        }
                        if (premiumCrossfadeDrawable[0] != null) {
                            premiumCrossfadeDrawable[0].setProgress(1f);
                        }
                        if (premiumCrossfadeDrawable[1] != null) {
                            premiumCrossfadeDrawable[1].setProgress(1f);
                        }
                        updateEmojiStatusDrawableColor(1f);
                        onlineTextView[1].setTextColor(0xB3FFFFFF);
                        actionBar.setItemsBackgroundColor(Theme.ACTION_BAR_WHITE_SELECTOR_COLOR, false);
                        actionBar.setItemsColor(Color.WHITE, false);
                        overlaysView.setOverlaysVisible();
                        overlaysView.setAlphaValue(1.0f, false);
                        avatarImage.setForegroundAlpha(1.0f);
                        avatarContainer.setVisibility(View.INVISIBLE);
                        avatarsViewPager.resetCurrentItem();
                        avatarsViewPager.setVisibility(View.VISIBLE);
                        if (showStatusButton != null) {
                            showStatusButton.setBackgroundColor(0x23ffffff);
                        }
                        if (storyView != null) {
                            storyView.setExpandProgress(1f);
                        }
                        if (giftsView != null) {
                            giftsView.setExpandProgress(1f);
                        }
                        if (actionsView != null) {
                            actionsView.setParentExpanded(1f);
                        }
                        if (musicView != null) {
                            musicView.setParentExpanded(1f);
                        }
                        if (ratingView != null) {
                            ratingView.setParentExpanded(1f);
                        }
                        expandPhoto = false;
                        updateCollectibleHint();
                    }

                    calculatePositionsOnFirstLoad();

                    allowPullingDown = true;
                    isPulledDown = true;
                    NotificationCenter.getGlobalInstance().postNotificationName(NotificationCenter.needCheckSystemBarColors, true);
                    if (otherItem != null) {
                        if (!isPeerNoForwards()) {
                            otherItem.showSubItem(gallery_menu_save);
                        } else {
                            otherItem.hideSubItem(gallery_menu_save);
                        }
                        if (imageUpdater != null) {
                            otherItem.showSubItem(edit_avatar);
                            otherItem.showSubItem(delete_avatar);
                            otherItem.hideSubItem(logout);
                        }
                    }
                    currentExpanAnimatorFracture = 1.0f;

                    int paddingTop;
                    int paddingBottom;
                    if (isInLandscapeMode) {
                        paddingTop = getHeaderExtraHeight() + actionBarHeight;
                        paddingBottom = 0;
                    } else {
                        paddingTop = listView.getMeasuredWidth() + getActionsExtraHeight();
                        paddingBottom = Math.max(0, getMeasuredHeight() - (listContentHeight + getHeaderExtraHeight() + actionBarHeight));
                    }
                    if (banFromGroup != 0) {
                        paddingBottom += dp(48) + AndroidUtilities.navigationBarHeight;
                        listView.setBottomGlowOffset(dp(48) + AndroidUtilities.navigationBarHeight);
                    } else {
                        listView.setBottomGlowOffset(0);
                    }
                    initialAnimationExtraHeight = paddingTop - actionBarHeight;
                    if (playProfileAnimation == 0) {
                        extraHeight = initialAnimationExtraHeight;
                    }
                    layoutManager.scrollToPositionWithOffset(0, -actionBarHeight);
                    listView.setPadding(0, paddingTop, 0, paddingBottom);
                    measureChildWithMargins(listView, widthMeasureSpec, 0, heightMeasureSpec, 0);
                    listView.layout(0, actionBarHeight, listView.getMeasuredWidth(), actionBarHeight + listView.getMeasuredHeight());
                    ignoreLayout = false;
                } else if (fragmentOpened && !openAnimationInProgress && !firstLayout) {
                    ignoreLayout = true;

                    int paddingTop;
                    int paddingBottom;
                    if (!hasMainTabs && (isInLandscapeMode || AndroidUtilities.isTablet())) {
                        paddingTop = getHeaderExtraHeight();
                        paddingBottom = 0;
                    } else {
                        paddingTop = listView.getMeasuredWidth() + getActionsExtraHeight();
                        paddingBottom = Math.max(0, getMeasuredHeight() - (listContentHeight + getHeaderExtraHeight() + actionBarHeight));
                    }
                    if (banFromGroup != 0) {
                        paddingBottom += AndroidUtilities.dp(48) + AndroidUtilities.navigationBarHeight;
                        listView.setBottomGlowOffset(AndroidUtilities.dp(48) + AndroidUtilities.navigationBarHeight);
                    } else {
                        listView.setBottomGlowOffset(0);
                    }
                    int currentPaddingTop = listView.getPaddingTop();
                    View view = null;
                    int pos = RecyclerView.NO_POSITION;
                    for (int i = 0; i < listView.getChildCount(); i++) {
                        int p = listView.getChildAdapterPosition(listView.getChildAt(i));
                        if (p != RecyclerView.NO_POSITION) {
                            view = listView.getChildAt(i);
                            pos = p;
                            break;
                        }
                    }
                    if (view == null) {
                        view = listView.getChildAt(0);
                        if (view != null) {
                            RecyclerView.ViewHolder holder = listView.findContainingViewHolder(view);
                            pos = holder.getAdapterPosition();
                            if (pos == RecyclerView.NO_POSITION) {
                                pos = holder.getPosition();
                            }
                        }
                    }

                    int top = paddingTop;
                    if (view != null) {
                        top = view.getTop();
                    }
                    boolean layout = false;
                    if ((actionBar.isSearchFieldVisible() || openSimilar) && sharedMediaRow >= 0) {
                        layoutManager.scrollToPositionWithOffset(sharedMediaRow, -paddingTop);
                        layout = true;
                    } else if (invalidateScroll || currentPaddingTop != paddingTop) {
                        if (savedScrollPosition >= 0) {
                            layoutManager.scrollToPositionWithOffset(savedScrollPosition, savedScrollOffset - paddingTop);
                        } else if ((!changed || !allowPullingDown) && view != null) {
                            if (pos == 0 && !allowPullingDown && top > getHeaderExtraHeight()) {
                                top = getHeaderExtraHeight();
                            }
                            layoutManager.scrollToPositionWithOffset(pos, top - paddingTop);
                            layout = true;
                        } else {
                            layoutManager.scrollToPositionWithOffset(0, getHeaderExtraHeight() - paddingTop);
                        }
                    }
                    if (currentPaddingTop != paddingTop || listView.getPaddingBottom() != paddingBottom) {
                        listView.setPadding(0, paddingTop, 0, paddingBottom);
                        layout = true;
                    }
                    if (layout) {
                        measureChildWithMargins(listView, widthMeasureSpec, 0, heightMeasureSpec, 0);
                        try {
                            listView.layout(0, actionBarHeight, listView.getMeasuredWidth(), actionBarHeight + listView.getMeasuredHeight());
                        } catch (Exception e) {
                            FileLog.e(e);
                        }
                    }
                    ignoreLayout = false;
                }

                boolean portrait = height > MeasureSpec.getSize(widthMeasureSpec);
                if (portrait != wasPortrait) {
                    post(() -> {
                        if (selectAnimatedEmojiDialog != null) {
                            selectAnimatedEmojiDialog.dismiss();
                            selectAnimatedEmojiDialog = null;
                        }
                    });
                    wasPortrait = portrait;
                }
            }

            @Override
            protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
                super.onLayout(changed, left, top, right, bottom);
                savedScrollPosition = -1;
                firstLayout = false;
                invalidateScroll = false;
                checkListViewScroll();
            }

            @Override
            public void requestLayout() {
                if (ignoreLayout) {
                    return;
                }
                super.requestLayout();
            }

            private final ArrayList<View> sortedChildren = new ArrayList<>();
            private final Comparator<View> viewComparator = (view, view2) -> (int) (view.getY() - view2.getY());


            @Override
            protected void dispatchDraw(Canvas canvas) {
                if (Build.VERSION.SDK_INT >= 31 && scrollableViewNoiseSuppressor != null) {
                    blur3_InvalidateBlur();

                    final int width = getMeasuredWidth();
                    final int height = getMeasuredHeight();
                    if (iBlur3SourceGlass != null && !iBlur3SourceGlass.inRecording()) {
                        //if (iBlur3SourceGlass.needUpdateDisplayList(width, height) || iBlur3Invalidated) {
                        final Canvas c = iBlur3SourceGlass.beginRecording(width, height);
                        c.drawColor(getThemedColor(Theme.key_windowBackgroundGray));
                        if (SharedConfig.chatBlurEnabled()) {
                            scrollableViewNoiseSuppressor.draw(c, DownscaleScrollableNoiseSuppressor.DRAW_GLASS);
                        }
                        iBlur3SourceGlass.endRecording();
                        //}
                    }
                    iBlur3Invalidated = false;
                }

                whitePaint.setColor(getThemedColor(Theme.key_windowBackgroundGray));
                if (listView.getVisibility() == VISIBLE) {
                    grayPaint.setColor(getThemedColor(Theme.key_windowBackgroundGray));
                    if (transitionAnimationInProress) {
                        whitePaint.setAlpha((int) (255 * listView.getAlpha()));
                    }
                    if (transitionAnimationInProress) {
                        grayPaint.setAlpha((int) (255 * listView.getAlpha()));
                    }

                    int count = listView.getChildCount();
                    sortedChildren.clear();
                    boolean hasRemovingItems = false;
                    for (int i = 0; i < count; i++) {
                        View child = listView.getChildAt(i);
                        if (listView.getChildAdapterPosition(child) != RecyclerView.NO_POSITION) {
                            sortedChildren.add(listView.getChildAt(i));
                        } else {
                            hasRemovingItems = true;
                        }
                    }
                    Collections.sort(sortedChildren, viewComparator);
                    boolean hasBackground = false;
                    float lastY = listView.getY();
                    count = sortedChildren.size();
                    if (!openAnimationInProgress && count > 0 && !hasRemovingItems) {
                        lastY += sortedChildren.get(0).getY();
                    }
                    float alpha = 1f;
                    for (int i = 0; i < count; i++) {
                        View child = sortedChildren.get(i);
                        boolean currentHasBackground = child.getBackground() != null;
                        int currentY = (int) (listView.getY() + child.getY());
                        if (hasBackground == currentHasBackground) {
                            if (child.getAlpha() == 1f) {
                                alpha = 1f;
                            }
                            continue;
                        }
                        if (hasBackground) {
//                            canvas.drawRect(listView.getX(), lastY, listView.getX() + listView.getMeasuredWidth(), currentY, grayPaint);
                        } else {
                            if (alpha != 1f) {
//                                canvas.drawRect(listView.getX(), lastY, listView.getX() + listView.getMeasuredWidth(), currentY, grayPaint);
//                                whitePaint.setAlpha((int) (255 * alpha));
//                                canvas.drawRect(listView.getX(), lastY, listView.getX() + listView.getMeasuredWidth(), currentY, whitePaint);
//                                whitePaint.setAlpha(255);
                            } else {
//                                canvas.drawRect(listView.getX(), lastY, listView.getX() + listView.getMeasuredWidth(), currentY, whitePaint);
                            }
                        }
                        hasBackground = currentHasBackground;
                        lastY = currentY;
                        alpha = child.getAlpha();
                    }

                    if (hasBackground) {
//                        canvas.drawRect(listView.getX(), lastY, listView.getX() + listView.getMeasuredWidth(), listView.getBottom(), grayPaint);
                    } else {
                        if (alpha != 1f) {
//                            canvas.drawRect(listView.getX(), lastY, listView.getX() + listView.getMeasuredWidth(), listView.getBottom(), grayPaint);
//                            whitePaint.setAlpha((int) (255 * alpha));
//                            canvas.drawRect(listView.getX(), lastY, listView.getX() + listView.getMeasuredWidth(), listView.getBottom(), whitePaint);
//                            whitePaint.setAlpha(255);
                        } else {
//                            canvas.drawRect(listView.getX(), lastY, listView.getX() + listView.getMeasuredWidth(), listView.getBottom(), whitePaint);
                        }
                    }
                } else {
//                    int top = searchListView.getTop();
//                    canvas.drawRect(0, top + extraHeight + searchTransitionOffset, getMeasuredWidth(), top + getMeasuredHeight(), whitePaint);
                }
                super.dispatchDraw(canvas);
                /*
                if (profileTransitionInProgress && parentLayout.getFragmentStack().size() > 1) {
                    BaseFragment fragment = parentLayout.getFragmentStack().get(parentLayout.getFragmentStack().size() - 2);
                    if (fragment instanceof ChatActivity) {
                        ChatActivity chatActivity = (ChatActivity) fragment;
                        FragmentContextView fragmentContextView = chatActivity.getFragmentContextView();

                        if (fragmentContextView != null && fragmentContextView.isCallStyle()) {
                            float progress = extraHeight / AndroidUtilities.dpf2(fragmentContextView.getStyleHeight());
                            if (progress > 1f) {
                                progress = 1f;
                            }
                            canvas.save();
                            canvas.translate(fragmentContextView.getX(), fragmentContextView.getY());
                            fragmentContextView.setDrawOverlay(true);
                            fragmentContextView.setCollapseTransition(true, extraHeight, progress);
                            fragmentContextView.draw(canvas);
                            fragmentContextView.setCollapseTransition(false, extraHeight, progress);
                            fragmentContextView.setDrawOverlay(false);
                            canvas.restore();
                        }
                    }
                }
                */
                if (scrimPaint.getAlpha() > 0) {
                    canvas.drawRect(0, 0, getWidth(), getHeight(), scrimPaint);
                }
                if (scrimView != null) {
                    int c = canvas.save();
                    canvas.translate(scrimView.getLeft(), scrimView.getTop());
                    if (scrimView == actionBar.getBackButton()) {
                        int r = Math.max(scrimView.getMeasuredWidth(), scrimView.getMeasuredHeight()) / 2;
                        int wasAlpha = actionBarBackgroundPaint.getAlpha();
                        actionBarBackgroundPaint.setAlpha((int) (wasAlpha * (scrimPaint.getAlpha() / 255f) / 0.3f));
                        canvas.drawCircle(r, r, r * 0.7f, actionBarBackgroundPaint);
                        actionBarBackgroundPaint.setAlpha(wasAlpha);
                    }
                    scrimView.draw(canvas);
                    canvas.restoreToCount(c);
                }
                if (blurredView != null && blurredView.getVisibility() == View.VISIBLE) {
                    if (blurredView.getAlpha() != 1f) {
                        if (blurredView.getAlpha() != 0) {
                            canvas.saveLayerAlpha(blurredView.getLeft(), blurredView.getTop(), blurredView.getRight(), blurredView.getBottom(), (int) (255 * blurredView.getAlpha()), Canvas.ALL_SAVE_FLAG);
                            canvas.translate(blurredView.getLeft(), blurredView.getTop());
                            blurredView.draw(canvas);
                            canvas.restore();
                        }
                    } else {
                        blurredView.draw(canvas);
                    }
                }

                if (!hasMainTabs) {
                    canvas.save();
                    canvas.translate(getInternalTranslationX(), 0);
                    AndroidUtilities.drawNavigationBarProtection(canvas, this, getThemedColor(Theme.key_windowBackgroundWhite), navigationBarHeight, getInternalVisibility());
                    canvas.restore();
                }
            }

            @Override
            protected boolean drawChild(Canvas canvas, View child, long drawingTime) {
                if (pinchToZoomHelper.isInOverlayMode() && (child == avatarContainer2 || child == actionBar || child == writeButton)) {
                    return true;
                }
                if (child == blurredView) {
                    return true;
                }
                return super.drawChild(canvas, child, drawingTime);
            }

            @Override
            protected void onAttachedToWindow() {
                super.onAttachedToWindow();
                fragmentViewAttached = true;
                for (int i = 0; i < emojiStatusDrawable.length; i++) {
                    if (emojiStatusDrawable[i] != null) {
                        emojiStatusDrawable[i].attach();
                    }
                }
                for (int i = 0; i < botVerificationDrawable.length; ++i) {
                    if (botVerificationDrawable[i] != null) {
                        botVerificationDrawable[i].attach();
                    }
                }
            }

            @Override
            protected void onDetachedFromWindow() {
                super.onDetachedFromWindow();
                fragmentViewAttached = false;
                for (int i = 0; i < emojiStatusDrawable.length; i++) {
                    if (emojiStatusDrawable[i] != null) {
                        emojiStatusDrawable[i].detach();
                    }
                }
                for (int i = 0; i < botVerificationDrawable.length; ++i) {
                    if (botVerificationDrawable[i] != null) {
                        botVerificationDrawable[i].detach();
                    }
                }
            }
        };

        viewPositionWatcher = new ViewPositionWatcher(fragmentView);
        iBlur3FactoryLiquidGlass.setSourceRootView(viewPositionWatcher, (ViewGroup) fragmentView);

        FrameLayout frameLayout = (FrameLayout) fragmentView;

        if (myProfile) {
            bottomButtonsContainer = new FrameLayout(context);

            bottomButtonContainer = new FrameLayout[2];
            bottomButton = new ButtonWithCounterView[2];
            for (int a = 0; a < 2; ++a) {
                BlurredBackgroundSourceColor source = new BlurredBackgroundSourceColor();
                source.setColor(getThemedColor(Theme.key_windowBackgroundWhite));
                BlurredBackgroundDrawableViewFactory factory = new BlurredBackgroundDrawableViewFactory(source);
                Button2 button2 = new Button2(context);
                BlurredBackgroundDrawable drawable = factory.create(button2, new BlurredBackgroundColorProviderThemed(resourcesProvider, Theme.key_windowBackgroundWhite));
                drawable.setPadding(dp(8));
                drawable.setRadius(dp(22));
                button2.setBackground(drawable);

                bottomButtonContainer[a] = new FrameLayout(context);

                    bottomButton[a] = new ButtonWithCounterView(context, resourcesProvider);
                bottomButton[a].setRoundRadius(dp(19));
                bottomButton[a].setUseWrapContent(true);
                bottomButton[a].setPadding(dp(16), 0, dp(16), 0);
                if (a == 0) {
                    bottomButtonPostText = new SpannableStringBuilder("c");
                    bottomButtonPostText.setSpan(new ColoredImageSpan(R.drawable.filled_premium_camera), 0, 1, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                    bottomButtonPostText.append("  ").append(getString(R.string.StoriesAddPost));
                    bottomButtonPostTextAlbum = new SpannableStringBuilder("c");
                    bottomButtonPostTextAlbum.setSpan(new ColoredImageSpan(R.drawable.filled_add_album), 0, 1, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                    bottomButtonPostTextAlbum.append("  ").append(getString(R.string.StoriesAlbumBottomButtonAddStories));
                    bottomButton[a].setText(bottomButtonPostText, false);
                } else {
                    bottomButton[a].setText(getString(R.string.StorySave), false);
                }
                final int finalA = a;
                button2.setOnClickListener(v -> {
                    if (finalA == 0 && !sharedMediaLayout.isActionModeShown()) {
                        if (SharedMediaLayout.isStoryAlbumPageType(sharedMediaLayout.getClosestTab())) {
                            int albumId = sharedMediaLayout.storyAlbums_getAlbumIdByTabType(sharedMediaLayout.getClosestTab());
                            sharedMediaLayout.openAddStoriesToAlbumSheet(this, getDialogId(), albumId);
                        } else {
                            if (!getMessagesController().storiesEnabled()) {
                                showDialog(new PremiumFeatureBottomSheet(this, PremiumPreviewFragment.PREMIUM_FEATURE_STORIES, true));
                                return;
                            }
                            getMessagesController().getMainSettings().edit().putBoolean("story_keep", true).apply();
                            StoryRecorder.getInstance(getParentActivity(), getCurrentAccount())
                                    .closeToWhenSent(new StoryRecorder.ClosingViewProvider() {
                                        @Override
                                        public void preLayout(long dialogId, Runnable runnable) {
                                            avatarImage.setHasStories(needInsetForStories());
                                            if (dialogId == getDialogId()) {
                                                collapseAvatarInstant();
                                            }
                                            AndroidUtilities.runOnUIThread(runnable, 30);
                                        }

                                        @Override
                                        public StoryRecorder.SourceView getView(long dialogId) {
                                            if (dialogId != getDialogId()) {
                                                return null;
                                            }
                                            updateAvatarRoundRadius();
                                            return StoryRecorder.SourceView.fromAvatarImage(avatarImage, ChatObject.isForum(currentChat));
                                        }
                                    })
                                    .open(null);
                        }
                    } else {
                        if (SharedMediaLayout.isStoryAlbumPageType(sharedMediaLayout.getClosestTab())) {
                            final long dialogId = getDialogId();
                            final int albumId = sharedMediaLayout.storyAlbums_getAlbumIdByTabType(sharedMediaLayout.getClosestTab());
                            final String albumName = getMessagesController().getStoriesController().getAlbumName(dialogId, albumId);
                            if (applyBulletin != null) {
                                applyBulletin.run();
                                applyBulletin = null;
                            }
                            Bulletin.hideVisible();
                            int count = 0;
                            ArrayList<TL_stories.StoryItem> storyItems = new ArrayList<>();
                            SparseArray<MessageObject> actionModeMessageObjects = sharedMediaLayout.getActionModeSelected();
                            if (actionModeMessageObjects != null) {
                                for (int i = 0; i < actionModeMessageObjects.size(); ++i) {
                                    MessageObject messageObject = actionModeMessageObjects.valueAt(i);
                                    if (messageObject.storyItem != null) {
                                        storyItems.add(messageObject.storyItem);
                                        count++;
                                    }
                                }
                            }
                            sharedMediaLayout.closeActionMode(false);
                            if (storyItems.isEmpty()) {
                                return;
                            }

                            Runnable undo = () -> getMessagesController().getStoriesController().addStoriesToAlbum(dialogId, albumId, storyItems);
                            getMessagesController().getStoriesController().removeStoriesFromAlbum(dialogId, albumId, storyItems);
                            BulletinFactory.of(this).createSimpleBulletin(
                                    R.raw.chats_archived,
                                    AndroidUtilities.replaceTags(LocaleController.formatPluralString("StoryRemovedFromAlbumTitle", storyItems.size(), albumName)),
                                    LocaleController.getString(R.string.UndoNoCaps), undo
                            ).show();
                        } else {
                            final long dialogId = getUserConfig().getClientUserId();
                            if (applyBulletin != null) {
                                applyBulletin.run();
                                applyBulletin = null;
                            }
                            Bulletin.hideVisible();
                            boolean pin = sharedMediaLayout.getClosestTab() == SharedMediaLayout.TAB_ARCHIVED_STORIES;
                            int count = 0;
                            ArrayList<TL_stories.StoryItem> storyItems = new ArrayList<>();
                            SparseArray<MessageObject> actionModeMessageObjects = sharedMediaLayout.getActionModeSelected();
                            if (actionModeMessageObjects != null) {
                                for (int i = 0; i < actionModeMessageObjects.size(); ++i) {
                                    MessageObject messageObject = actionModeMessageObjects.valueAt(i);
                                    if (messageObject.storyItem != null) {
                                        storyItems.add(messageObject.storyItem);
                                        count++;
                                    }
                                }
                            }
                            sharedMediaLayout.closeActionMode(false);
                            if (pin) {
                                sharedMediaLayout.scrollToPage(SharedMediaLayout.TAB_STORIES);
                            }
                            if (storyItems.isEmpty()) {
                                return;
                            }
                            boolean[] pastValues = new boolean[storyItems.size()];
                            for (int i = 0; i < storyItems.size(); ++i) {
                                TL_stories.StoryItem storyItem = storyItems.get(i);
                                pastValues[i] = storyItem.pinned;
                                storyItem.pinned = pin;
                            }
                            getMessagesController().getStoriesController().updateStoriesInLists(dialogId, storyItems);
                            final boolean[] undone = new boolean[]{false};
                            applyBulletin = () -> {
                                getMessagesController().getStoriesController().updateStoriesPinned(dialogId, storyItems, pin, null);
                            };
                            final Runnable undo = () -> {
                                undone[0] = true;
                                AndroidUtilities.cancelRunOnUIThread(applyBulletin);
                                for (int i = 0; i < storyItems.size(); ++i) {
                                    TL_stories.StoryItem storyItem = storyItems.get(i);
                                    storyItem.pinned = pastValues[i];
                                }
                                getMessagesController().getStoriesController().updateStoriesInLists(dialogId, storyItems);
                            };
                            Bulletin bulletin;
                            if (pin) {
                                bulletin = BulletinFactory.of(this).createSimpleBulletin(R.raw.contact_check, LocaleController.formatPluralString("StorySavedTitle", count), LocaleController.getString(R.string.StorySavedSubtitle), LocaleController.getString(R.string.UndoNoCaps), undo).show();
                            } else {
                                bulletin = BulletinFactory.of(this).createSimpleBulletin(R.raw.chats_archived, LocaleController.formatPluralString("StoryArchived", count), LocaleController.getString(R.string.UndoNoCaps), Bulletin.DURATION_PROLONG, undo).show();
                            }
                            bulletin.setOnHideListener(() -> {
                                if (!undone[0] && applyBulletin != null) {
                                    applyBulletin.run();
                                }
                                applyBulletin = null;
                            });
                        }
                    }
                });
                ScaleStateListAnimator.apply(button2, .02f, 1.2f);
                bottomButton[a].setStateListAnimator(null);
                button2.addView(bottomButton[a], LayoutHelper.createFrame(LayoutHelper.WRAP_CONTENT, LayoutHelper.MATCH_PARENT, Gravity.CENTER));
                bottomButtonContainer[a].addView(button2, LayoutHelper.createFrame(LayoutHelper.WRAP_CONTENT, 60, Gravity.CENTER_HORIZONTAL));
                bottomButtonsContainer.addView(bottomButtonContainer[a], LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.MATCH_PARENT, Gravity.BOTTOM | Gravity.FILL_HORIZONTAL));
                if (a == 1 || !getMessagesController().storiesEnabled()) {
                    bottomButtonContainer[a].setTranslationY(dp(72));
                }
            }
        }

        final ArrayList<Integer> users = chatInfo != null && chatInfo.participants != null && chatInfo.participants.participants.size() > 5 ? sortedUsers : null;
        int initialTab = -1;
        if (openCommonChats) {
            initialTab = SharedMediaLayout.TAB_COMMON_GROUPS;
        } else if (openGifts && (userInfo != null && userInfo.stargifts_count > 0 || chatInfo != null && chatInfo.stargifts_count > 0)) {
            initialTab = SharedMediaLayout.TAB_GIFTS;
            openedGifts = true;
        } else if (openSimilar) {
            initialTab = SharedMediaLayout.TAB_RECOMMENDED_CHANNELS;
        } else if (users != null) {
            initialTab = SharedMediaLayout.TAB_GROUPUSERS;
        }
        sharedMediaLayout = new SharedMediaLayout(context, did, sharedMediaPreloader, userInfo != null ? userInfo.common_chats_count : 0, sortedUsers, chatInfo, userInfo, initialTab, initialStoryAlbum, this, this, SharedMediaLayout.VIEW_TYPE_PROFILE_ACTIVITY, resourcesProvider, iBlur3FactoryLiquidGlass) {
            @Override
            protected int processColor(int color) {
                return dontApplyPeerColor(color, false);
            }

            @Override
            protected void onSelectedTabChanged() {
                updateSelectedMediaTabText();
            }

            @Override
            protected boolean includeSavedDialogs() {
                return dialogId == getUserConfig().getClientUserId() && !saved;
            }

            @Override
            protected boolean isSelf() {
                return myProfile;
            }

            @Override
            protected boolean isStoriesView() {
                return myProfile;
            }

            @Override
            protected void onSearchStateChanged(boolean expanded) {
                AndroidUtilities.removeAdjustResize(getParentActivity(), classGuid);

                listView.stopScroll();
                avatarContainer2.setPivotY(avatarContainer.getPivotY() + avatarContainer.getMeasuredHeight() / 2f);
                avatarContainer2.setPivotX(avatarContainer2.getMeasuredWidth() / 2f);
                AndroidUtilities.updateViewVisibilityAnimated(avatarContainer2, !expanded, 0.95f, true);

                callItem.setVisibility(expanded || !callItemVisible ? GONE : INVISIBLE);
                videoCallItem.setVisibility(expanded || !videoCallItemVisible ? GONE : INVISIBLE);
                editItem.setVisibility(expanded || !editItemVisible ? GONE : INVISIBLE);
                otherItem.setVisibility(expanded ? GONE : INVISIBLE);
                updateStoriesViewBounds(false);
            }

            @Override
            protected boolean onMemberClick(TLRPC.ChatParticipant participant, boolean isLong, View view) {
                return ProfileActivity.this.onMemberClick(participant, isLong, view);
            }

            @Override
            protected void drawBackgroundWithBlur(Canvas canvas, float y, Rect rectTmp2, Paint backgroundPaint) {
                contentView.drawBlurRect(canvas, listView.getY() + getY() + y, rectTmp2, backgroundPaint, true);
            }

            @Override
            protected void invalidateBlur() {
                if (contentView != null) {
                    contentView.invalidateBlur();
                }
            }

            @Override
            protected int getInitialTab() {
                return TAB_STORIES;
            }

            @Override
            protected void showActionMode(boolean show) {
                super.showActionMode(show);
                if (myProfile) {
                    disableScroll(show);

                    int a = getSelectedTab() - SharedMediaLayout.TAB_STORIES;
                    if (a < 0 || a > 1) return;
                    bottomButtonContainer[a]
                            .animate()
                            .translationY(show || a == 0 && MessagesController.getInstance(currentAccount).storiesEnabled() ? 0 : dp(72))
                            .setDuration(320)
                            .setInterpolator(CubicBezierInterpolator.EASE_OUT_QUINT)
                            .setUpdateListener(anm -> updateBottomButtonY())
                            .start();
                }
            }

            @Override
            protected void onTabProgress(float progress) {
                super.onTabProgress(progress);
                if (sharedMediaLayout == null) {
                    return;
                }

                if (myProfile) {
                    if (bottomButtonContainer[0] != null) {
                        final float x = sharedMediaLayout.getTabTranslationX(SharedMediaLayout.TAB_STORIES, true);
                        bottomButtonContainer[0].setTranslationX(x);
                    }
                    if (bottomButtonContainer[1] != null) {
                        final float x = sharedMediaLayout.getTabTranslationX(SharedMediaLayout.TAB_ARCHIVED_STORIES, false);
                        bottomButtonContainer[1].setTranslationX(x);
                    }
                    checkStoriesButtonText(lastStoriesSelectedCount, true);
                    updateBottomButtonY();
                }
            }

            @Override
            protected void onBottomButtonVisibilityChange() {
                super.onBottomButtonVisibilityChange();
                if (myProfile && bottomButtonContainer[0] != null && sharedMediaLayout != null) {
                    bottomButtonContainer[0].setTranslationY(dp(72) * (1f - sharedMediaLayout.getBottomButtonStoriesVisibility()));
                }
            }

            @Override
            protected void onAttachedToWindow() {
                super.onAttachedToWindow();
                AndroidUtilities.runOnUIThread(() -> {
                    onTabProgress(getTabProgress());
                });
            }

            @Override
            protected void onActionModeSelectedUpdate(SparseArray<MessageObject> messageObjects) {
                super.onActionModeSelectedUpdate(messageObjects);
                if (myProfile) {
                    final int count = messageObjects.size();

                    int type = getSelectedTab();
                    int a = (SharedMediaLayout.isStoryAlbumPageType(type) || type == SharedMediaLayout.TAB_STORIES) ? 0 :
                        (type == SharedMediaLayout.TAB_ARCHIVED_STORIES ? 1 : -1);
                    if (a < 0 || a > 1) return;
                    if (a == 0) {
                        checkStoriesButtonText(count, true);
                    }
                    bottomButton[a].setCount(count, true);
                }
            }

            private boolean openedGiftsCollection;
            @Override
            public void updateTabs(boolean animated) {
                super.updateTabs(animated);
                if (openGifts && !openedGifts && scrollSlidingTextTabStrip.hasTab(TAB_GIFTS)) {
                    if (!openedGiftsCollection && openGiftsCollection > 0 && giftsContainer != null) {
                        openedGiftsCollection = true;
                        giftsContainer.scrollToCollectionId(openGiftsCollection);
                    }
                    openedGifts = true;
                    scrollToPage(TAB_GIFTS);
                } else if (openGifts && openedGifts && !openedGiftsCollection && openGiftsCollection > 0 && giftsContainer != null) {
                    openedGiftsCollection = true;
                    giftsContainer.scrollToCollectionId(openGiftsCollection);
                }
            }
        };
        sharedMediaLayout.setLayoutParams(new RecyclerView.LayoutParams(RecyclerView.LayoutParams.MATCH_PARENT, RecyclerView.LayoutParams.MATCH_PARENT));
        sharedMediaLayout.initBlurCapture((ViewGroup) fragmentView);
//        sharedMediaLayout.setBackgroundColor(getThemedColor(Theme.key_windowBackgroundWhite));
        if (userId == 0 || imageUpdater == null || myProfile) {
            musicView = new ProfileMusicView(context, resourcesProvider);
            if (avatarsBlurView != null) {
                avatarsBlurView.setMusicView(musicView);
            }
            musicView.setColor(peerColor);
            if (userInfo != null && userInfo.saved_music != null) {
                musicView.setMusicDocument(userInfo.saved_music);
            }
            musicView.setOnClickListener(v -> {
                if (savedMusicList == null) {
                    if (
                        MediaController.getInstance().currentSavedMusicList != null &&
                        MediaController.getInstance().currentSavedMusicList.currentAccount == currentAccount &&
                        MediaController.getInstance().currentSavedMusicList.dialogId == getDialogId()) {
                        savedMusicList = MediaController.getInstance().currentSavedMusicList;
                    } else {
                        savedMusicList = new MessagesController.SavedMusicList(currentAccount, getDialogId());
                        if (userInfo != null && userInfo.saved_music != null) {
                            savedMusicList.setup(userInfo.saved_music);
                        }
                    }
                }
                if (!savedMusicList.list.isEmpty()) {
                    boolean sameList = false;
                    if (
                        MediaController.getInstance().currentSavedMusicList != savedMusicList ||
                        !MediaController.getInstance().isPlayingMessage(savedMusicList.list.get(0))
                    ) {
                        MediaController.getInstance().cleanup();
                    } else {
                        sameList = true;
                    }
                    MediaController.getInstance().currentSavedMusicList = savedMusicList;
                    MediaController.getInstance().getPlaylist().clear();
                    MediaController.getInstance().getPlaylist().addAll(savedMusicList.list);
                    if (!sameList) MediaController.getInstance().playMessage(savedMusicList.list.get(0));
                    showDialog(new AudioPlayerAlert(getContext(), getResourceProvider()));
                }
            });

            actionsView = new ProfileActionsView(context, dp(74)) {
                @Override
                public void setTranslationY(float translationY) {
                    super.setTranslationY(translationY);
                }
            };
            setActionsMode();
            updateNotifications(false);

            actionsView.setOnActionClickListener((key, x, y) -> {
                switch (key) {
                    case ProfileActionsView.KEY_GIFT:
                        onGiftPermiumClicked();
                        break;
                    case ProfileActionsView.KEY_SHARE:
                        onShareClicked();
                        break;
                    case ProfileActionsView.KEY_NOTIFICATION:
                        onNotificationsClicked(true, x, y, actionsView);
                        break;
                    case ProfileActionsView.KEY_MESSAGE:
                        if (isTopic) {
                            openTopic();
                        } else if (userId != 0) {
                            openChat();
                        } else if (chatId != 0) {
                            TLRPC.Chat chatLocal = getMessagesController().getChat(chatId);
                            if (ChatObject.isForum(chatLocal)) {
                                openForum();
                            } else {
                                openGroup();
                            }
                        }
                        break;
                    case ProfileActionsView.KEY_DISCUSS:
                        openDiscussion();
                        break;
                    case ProfileActionsView.KEY_LEAVE:
                        leaveChatPressed(false);
                        break;
                    case ProfileActionsView.KEY_JOIN:
                        onJoinClicked(true);
                        break;
                    case ProfileActionsView.KEY_REPORT:
                        ReportBottomSheet.openChat(this, getDialogId());
                        break;
                    case ProfileActionsView.KEY_CALL:
                    case ProfileActionsView.KEY_VOICE_CHAT:
                    case ProfileActionsView.KEY_STREAM:
                        onCallClicked(false);
                        break;
                    case ProfileActionsView.KEY_VIDEO:
                        onCallClicked(true);
                        break;
                    case ProfileActionsView.KEY_STORY:
                        getMessagesController().getMainSettings().edit().putBoolean("story_keep", true).apply();
                        final AlertDialog progressDialog = new AlertDialog(getContext(), AlertDialog.ALERT_TYPE_SPINNER, resourcesProvider);
                        progressDialog.showDelayed(200);
                        MessagesController.getInstance(currentAccount).getStoriesController().canSendStoryFor(getDialogId(), aBoolean -> {
                            progressDialog.dismiss();
                            if (aBoolean) {
                                StoryRecorder.getInstance(getParentActivity(), getCurrentAccount())
                                    .selectedPeerId(getDialogId())
                                    .open(null);
                            }
                        }, true, resourcesProvider);
                        break;
                    case ProfileActionsView.KEY_STOP:
                        onBlockContactClicked(true);
                        break;
                    case ProfileActionsView.KEY_SET_PHOTO:
                        onWriteButtonClick();
                        break;
                    case ProfileActionsView.KEY_EDIT_INFO:
                        presentFragment(new UserInfoActivity());
                        break;
                    case ProfileActionsView.KEY_EDIT_USERNAME:
                        TLRPC.User user = getUserConfig().getCurrentUser();
                        if (user == null) return;
                        ItemOptions itemOptions = ItemOptions.makeOptions(this, actionsView);
                        itemOptions.setGravity(Gravity.LEFT);
                        itemOptions.add(R.drawable.msg_qrcode, getString(R.string.QrCode), () -> {
                            Bundle args = new Bundle();
                            args.putLong("chat_id", chatId);
                            args.putLong("user_id", userId);
                            presentFragment(new QrActivity(args));
                        });
                        itemOptions.add(R.drawable.msg_copy, getString(R.string.ProfileCopyUsername), () -> {
                            AndroidUtilities.addToClipboard("@" + UserObject.getPublicUsername(user));
                        });
                        itemOptions.add(R.drawable.msg_edit, getString(R.string.ProfileUsernameEdit), () -> {
                            presentFragment(new ChangeUsernameActivity());
                        });
                        itemOptions.forceBottom(true);
                        itemOptions.translate(x - dp(8), actionsView.getMeasuredHeight() - dp(16));
                        itemOptions.show();
                        break;
                    case ProfileActionsView.KEY_SETTINGS:
                        presentFragment(new SettingsActivity());
                        break;
                }
            });
        }

        ActionBarMenu menu = actionBar.createMenu();

        if (userId == getUserConfig().clientUserId && !myProfile) {
            if (ContactsController.getInstance(currentAccount).getPrivacyRules(PRIVACY_RULES_TYPE_ADDED_BY_PHONE) == null) {
                ContactsController.getInstance(currentAccount).loadPrivacySettings();
            }
        }
        if (imageUpdater != null && !myProfile) {
            searchItem = menu.addItem(search_button, R.drawable.outline_header_search).setIsSearchField(true).setActionBarMenuItemSearchListener(new ActionBarMenuItem.ActionBarMenuItemSearchListener() {

                @Override
                public Animator getCustomToggleTransition() {
                    searchMode = !searchMode;
                    if (!searchMode) {
                        searchItem.clearFocusOnSearchView();
                    }
                    if (searchMode) {
                        searchItem.getSearchField().setText("");
                    }
                    return searchExpandTransition(searchMode);
                }

                @Override
                public void onTextChanged(EditText editText) {
                    searchAdapter.search(editText.getText().toString().toLowerCase());
                }
            });
            searchItem.setContentDescription(LocaleController.getString(R.string.SearchInSettings));
            searchItem.setSearchFieldHint(LocaleController.getString(R.string.SearchInSettings));
            sharedMediaLayout.getSearchItem().setVisibility(View.GONE);
            if (sharedMediaLayout.getSearchOptionsItem() != null) {
                sharedMediaLayout.getSearchOptionsItem().setVisibility(View.GONE);
            }
            if (sharedMediaLayout.getSaveItem() != null) {
                sharedMediaLayout.getSaveItem().setVisibility(View.GONE);
            }
            if (expandPhoto) {
                searchItem.setVisibility(View.GONE);
            }
        }

        videoCallItem = menu.addItem(video_call_item, R.drawable.profile_video);
        videoCallItem.setContentDescription(LocaleController.getString(R.string.VideoCall));
        if (chatId != 0) {
            callItem = menu.addItem(call_item, R.drawable.msg_voicechat2);
            if (ChatObject.isChannelOrGiga(currentChat)) {
                callItem.setContentDescription(LocaleController.getString(R.string.VoipChannelVoiceChat));
            } else {
                callItem.setContentDescription(LocaleController.getString(R.string.VoipGroupVoiceChat));
            }
        } else {
            callItem = menu.addItem(call_item, R.drawable.call);
            callItem.setContentDescription(LocaleController.getString(R.string.Call));
        }
        if (myProfile) {
            editItem = menu.addItem(edit_profile, R.drawable.group_edit_profile);
            editItem.setContentDescription(LocaleController.getString(R.string.Edit));
        } else {
            editItem = menu.addItem(edit_channel, R.drawable.group_edit_profile);
            editItem.setContentDescription(LocaleController.getString(R.string.Edit));
        }
        otherItem = menu.addItem(10, R.drawable.ic_ab_other, resourcesProvider);
        ttlIconView = new ImageView(context);
        ttlIconView.setColorFilter(new PorterDuffColorFilter(getThemedColor(Theme.key_actionBarDefaultIcon), PorterDuff.Mode.MULTIPLY));
        AndroidUtilities.updateViewVisibilityAnimated(ttlIconView, false, 0.8f, false);
        ttlIconView.setImageResource(R.drawable.msg_mini_autodelete_timer);
        otherItem.addView(ttlIconView, LayoutHelper.createFrame(12, 12, Gravity.CENTER_VERTICAL | Gravity.LEFT, 8, 2, 0, 0));
        otherItem.setContentDescription(LocaleController.getString(R.string.AccDescrMoreOptions));

        int scrollTo;
        int scrollToPosition = 0;
        Object writeButtonTag = null;
        if (listView != null && imageUpdater != null) {
            scrollTo = layoutManager.findFirstVisibleItemPosition();
            View topView = layoutManager.findViewByPosition(scrollTo);
            if (topView != null) {
                scrollToPosition = topView.getTop() - listView.getPaddingTop();
            } else {
                scrollTo = -1;
            }
            writeButtonTag = writeButton.getTag();
        } else {
            scrollTo = -1;
        }

        createActionBarMenu(false);

        listAdapter = new ListAdapter(context);
        searchAdapter = new SearchAdapter(this, context);
        avatarDrawable = new AvatarDrawable();
        avatarDrawable.setScaleSize(100f / 42f);
        avatarDrawable.setProfile(true);

        fragmentView.setWillNotDraw(false);
        contentView = ((NestedFrameLayout) fragmentView);
        contentView.needBlur = true;

        listView = new ClippedListView(context, resourcesProvider) {

            private VelocityTracker velocityTracker;

            @Override
            protected boolean canHighlightChildAt(View child, float x, float y) {
                return !(child instanceof AboutLinkCell);
            }

            @Override
            protected boolean allowSelectChildAtPosition(View child) {
                return child != sharedMediaLayout;
            }

            @Override
            public boolean hasOverlappingRendering() {
                return false;
            }

            @Override
            protected void requestChildOnScreen(View child, View focused) {

            }

            @Override
            public void invalidate() {
                super.invalidate();
                if (fragmentView != null) {
                    fragmentView.invalidate();
                }
            }

            @Override
            public boolean onInterceptTouchEvent(MotionEvent e) {
                if (sharedMediaLayout != null) {
                    if (sharedMediaLayout.canEditStories() && sharedMediaLayout.isActionModeShown() && sharedMediaLayout.getClosestTab() == SharedMediaLayout.TAB_BOT_PREVIEWS) {
                        return false;
                    }
                    if (sharedMediaLayout.canEditStories() && sharedMediaLayout.isActionModeShown() && (sharedMediaLayout.getClosestTab() == SharedMediaLayout.TAB_STORIES || SharedMediaLayout.isStoryAlbumPageType(sharedMediaLayout.getClosestTab()))) {
                        return false;
                    }
                    if (sharedMediaLayout.giftsContainer != null && sharedMediaLayout.giftsContainer.isReordering()) {
                        return false;
                    }
                    if (sharedMediaLayout.storiesContainer != null && sharedMediaLayout.storiesContainer.isReordering()) {
                        return false;
                    }
                }
                return super.onInterceptTouchEvent(e);
            }

            @Override
            public boolean onTouchEvent(MotionEvent e) {
                final int action = e.getAction();
                if (action == MotionEvent.ACTION_DOWN) {
                    if (velocityTracker == null) {
                        velocityTracker = VelocityTracker.obtain();
                    } else {
                        velocityTracker.clear();
                    }
                    velocityTracker.addMovement(e);
                } else if (action == MotionEvent.ACTION_MOVE) {
                    if (velocityTracker != null) {
                        velocityTracker.addMovement(e);
                        velocityTracker.computeCurrentVelocity(1000);
                        listViewVelocityY = velocityTracker.getYVelocity(e.getPointerId(e.getActionIndex()));
                    }
                } else if (action == MotionEvent.ACTION_UP || action == MotionEvent.ACTION_CANCEL) {
                    if (velocityTracker != null) {
                        if (action == MotionEvent.ACTION_UP) {
                            velocityTracker.addMovement(e);
                            velocityTracker.computeCurrentVelocity(1000);
                            listViewVelocityY = velocityTracker.getYVelocity(e.getPointerId(e.getActionIndex()));
                        }

                        velocityTracker.recycle();
                        velocityTracker = null;
                    }
                }
                boolean result = super.onTouchEvent(e);
                if (action == MotionEvent.ACTION_MOVE) {
                    final int actionBarHeight = ActionBar.getCurrentActionBarHeight() + (actionBar.getOccupyStatusBar() ? AndroidUtilities.statusBarHeight : 0);
                    final float fullExtraHeight;
                    if (isInLandscapeMode && !hasMainTabs) {
                        fullExtraHeight = getHeaderExtraHeight() + actionBarHeight;
                    } else {
                        fullExtraHeight = listView.getMeasuredWidth() + getActionsExtraHeight();
                    }
                    if (extraHeight >= fullExtraHeight - 1) {
                        openAvatar(true);
                        result = false;
                    }
                }
                if (action == MotionEvent.ACTION_UP || action == MotionEvent.ACTION_CANCEL) {
                    final View view = layoutManager.findViewByPosition(0);
                    if (view != null) {
                        if (justFullyExpanded) {
                            justFullyExpanded = false;
                            listView.canStopFlinger = true;
                        }
                        if (allowPullingDown) {
                            if (isPulledDown) {
                                final int actionBarHeight = ActionBar.getCurrentActionBarHeight() + (actionBar.getOccupyStatusBar() ? AndroidUtilities.statusBarHeight : 0);
                                listView.smoothScrollBy(0, view.getTop() - listView.getMeasuredWidth() - getActionsExtraHeight() + actionBarHeight, CubicBezierInterpolator.EASE_OUT_QUINT);
                            } else {
                                listView.smoothScrollBy(0, view.getTop() - getHeaderExtraHeight(), CubicBezierInterpolator.EASE_OUT_QUINT);
                            }
                        } else {
                            final boolean hasActions = getActionsExtraHeight() > 0;
                            if (hasActions && extraHeight > 0 && (extraHeight < getHeaderExtraHeight() * 0.6f || listViewVelocityY < -1000f) && extraHeight > getActionsExtraHeight() * .6f) {
                                listView.smoothScrollBy(0, (int) (extraHeight - getActionsExtraHeight()), CubicBezierInterpolator.EASE_OUT_QUINT);
                            } else if (hasActions && extraHeight > 0 && extraHeight < getActionsExtraHeight() * .6f) {
                                listView.smoothScrollBy(0, (int) (getActionsExtraHeight() - extraHeight), CubicBezierInterpolator.EASE_OUT_QUINT);
                            } else if (!hasActions && extraHeight > 0 && listViewVelocityY < -1000f) {
                                listView.smoothScrollBy(0, (int) extraHeight, CubicBezierInterpolator.EASE_OUT_QUINT);
                            } else if (extraHeight > 0) {
                                listView.smoothScrollBy(0, view.getTop() - getHeaderExtraHeight(), CubicBezierInterpolator.EASE_OUT_QUINT);
                            }
                        }
                    }
                }
                return result;
            }

            @Override
            protected void onLayout(boolean changed, int l, int t, int r, int b) {
                super.onLayout(changed, l, t, r, b);
                updateBottomButtonY();
            }
        };
        listView.setSections();
        listView.applyPaddingToSections = false;
        listView.setBackgroundColor(getThemedColor(Theme.key_windowBackgroundGray));
        listView.setVerticalScrollBarEnabled(false);
        final IBlur3Capture listViewCapture = new ViewGroupPartRenderer(listView, (ViewGroup) fragmentView, (canvas, child, drawingTime) -> {
            if (child == sharedMediaLayout) {
                return true;
            }
            return listView.drawChild(canvas, child, drawingTime);
        });
        ((ViewGroupPartRenderer) listViewCapture).ignoreBlurCap = true;

        DefaultItemAnimator defaultItemAnimator = new DefaultItemAnimator() {

            int animationIndex = -1;

            @Override
            protected void onAllAnimationsDone() {
                super.onAllAnimationsDone();
                AndroidUtilities.runOnUIThread(() -> {
                    getNotificationCenter().onAnimationFinish(animationIndex);
                });
            }

            @Override
            public void runPendingAnimations() {
                boolean removalsPending = !mPendingRemovals.isEmpty();
                boolean movesPending = !mPendingMoves.isEmpty();
                boolean changesPending = !mPendingChanges.isEmpty();
                boolean additionsPending = !mPendingAdditions.isEmpty();
                if (removalsPending || movesPending || additionsPending || changesPending) {
                    ValueAnimator valueAnimator = ValueAnimator.ofFloat(0, 1f);
                    valueAnimator.addUpdateListener(valueAnimator1 -> listView.invalidate());
                    valueAnimator.setDuration(getMoveDuration());
                    valueAnimator.start();
                    animationIndex = getNotificationCenter().setAnimationInProgress(animationIndex, null);
                }
                super.runPendingAnimations();
            }

            @Override
            protected long getAddAnimationDelay(long removeDuration, long moveDuration, long changeDuration) {
                return 0;
            }

            @Override
            protected void onMoveAnimationUpdate(RecyclerView.ViewHolder holder) {
                super.onMoveAnimationUpdate(holder);
                updateBottomButtonY();
            }
        };
        listView.setItemAnimator(defaultItemAnimator);
        defaultItemAnimator.setMoveDelay(0);
        defaultItemAnimator.setMoveDuration(320);
        defaultItemAnimator.setRemoveDuration(320);
        defaultItemAnimator.setAddDuration(320);
        defaultItemAnimator.setSupportsChangeAnimations(false);
        defaultItemAnimator.setDelayAnimations(false);
        defaultItemAnimator.setInterpolator(CubicBezierInterpolator.EASE_OUT_QUINT);
        listView.setClipToPadding(false);
        listView.setHideIfEmpty(false);

        layoutManager = new LinearLayoutManager(context) {

            @Override
            public boolean supportsPredictiveItemAnimations() {
                return imageUpdater != null;
            }

            @Override
            public int scrollVerticallyBy(int dy, RecyclerView.Recycler recycler, RecyclerView.State state) {
                final View view = layoutManager.findViewByPosition(0);
                if (view != null && !openingAvatar) {
                    final int canScroll = view.getTop() - getHeaderExtraHeight();
                    if (!allowPullingDown && canScroll > dy) {
                        dy = canScroll;
                        if (avatarsViewPager.hasImages() && avatarImage.getImageReceiver().hasNotThumb() && !AndroidUtilities.isAccessibilityScreenReaderEnabled() && (!isInLandscapeMode && !AndroidUtilities.isTablet() || hasMainTabs)) {
                            allowPullingDown = avatarBig == null;
                        }
                    } else if (allowPullingDown) {
                        if (dy >= canScroll) {
                            dy = canScroll;
                            allowPullingDown = false;
                        } else if (listView.getScrollState() == RecyclerListView.SCROLL_STATE_DRAGGING) {
                            if (!isPulledDown) {
                                dy /= 2;
                            }
                        }
                    }
                }
                if (justFullyExpanded && !listView.isFlingerWorking()) {
                    return 0;
                }

                return super.scrollVerticallyBy(dy, recycler, state);
            }
        };
        layoutManager.setOrientation(LinearLayoutManager.VERTICAL);
        layoutManager.mIgnoreTopPadding = false;
        listView.setLayoutManager(layoutManager);
        listView.setGlowColor(0);
        listView.setAdapter(listAdapter);
        frameLayout.addView(listView, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.MATCH_PARENT, Gravity.TOP | Gravity.LEFT));
        listView.setOnItemClickListener((view, position, x, y) -> {
            if (getParentActivity() == null) {
                return;
            }
            listView.stopScroll();
            if (position == affiliateRow) {
                TLRPC.User user = getMessagesController().getUser(userId);
                if (userInfo != null && userInfo.starref_program != null) {
                    final long selfId = getUserConfig().getClientUserId();
                    BotStarsController.getInstance(currentAccount).getConnectedBot(getContext(), selfId, userId, connectedBot -> {
                        if (connectedBot == null) {
                            ChannelAffiliateProgramsFragment.showConnectAffiliateAlert(context, currentAccount, userInfo.starref_program, getUserConfig().getClientUserId(), resourcesProvider, false);
                        } else {
                            ChannelAffiliateProgramsFragment.showShareAffiliateAlert(context, currentAccount, connectedBot, selfId, resourcesProvider);
                        }
                    });
                } else if (user != null && user.bot_can_edit) {
                    presentFragment(new AffiliateProgramFragment(userId));
                }
            } else if (position == notificationsSimpleRow) {
                boolean muted = getMessagesController().isDialogMuted(did, topicId);
                getNotificationsController().muteDialog(did, topicId, !muted);
                BulletinFactory.createMuteBulletin(ProfileActivity.this, !muted, null).show();
                updateExceptions();
                if (notificationsSimpleRow >= 0 && listAdapter != null) {
                    listAdapter.notifyItemChanged(notificationsSimpleRow);
                }
            } else if (position == addToContactsRow) {
                TLRPC.User user = getMessagesController().getUser(userId);
                Bundle args = new Bundle();
                args.putLong("user_id", user.id);
                args.putBoolean("addContact", true);
                args.putString("phone", vcardPhone);
                args.putString("first_name_card", vcardFirstName);
                args.putString("last_name_card", vcardLastName);
                openAddToContact(user, args);
            } else if (position == deleteReactionRow) {
                final AlertDialog dialog = AlertsCreator.showAlertWithCheckbox(getContext(),
                    LocaleController.getString(R.string.DeleteReaction),
                    LocaleController.getString(R.string.DeleteAlertReaction),
                    getString(R.string.DeleteAlertReactionAll),
                    LocaleController.getString(R.string.Delete), (checked) -> {
                        final long participantDid;
                        if (userId != 0) {
                            participantDid = userId;
                        } else if (chatId != 0) {
                            participantDid = -chatId;
                        } else {
                            participantDid = dialogId;
                        }

                        if (checked) {
                            MessagesController.getInstance(currentAccount)
                                .deleteAllReactionsFrom(reportReactionFromDialogId, participantDid);
                        } else {
                            MessagesController.getInstance(currentAccount)
                                .deleteReactionsFromMessage(reportReactionFromDialogId, participantDid, reportReactionMessageId);
                        }

                        reportReactionMessageId = 0;
                        updateListAnimated(false);

                        final Bulletin.LottieLayout layout = new Bulletin.LottieLayout(getContext(), resourcesProvider);
                        layout.setAnimation(R.raw.chats_infotip);
                        layout.textView.setText(LocaleController.getString(R.string.ReactionDeleteSent));
                        BulletinFactory.of(ProfileActivity.this).create(layout, Bulletin.DURATION_SHORT).show();
                    }, resourcesProvider, false
                );

                TextView button = (TextView) dialog.getButton(DialogInterface.BUTTON_POSITIVE);
                if (button != null) {
                    button.setTextColor(Theme.getColor(Theme.key_text_RedBold));
                }
            } else if (position == reportReactionRow) {
                AlertDialog.Builder builder = new AlertDialog.Builder(getParentActivity(), resourcesProvider);
                builder.setTitle(LocaleController.getString(R.string.ReportReaction2));
                builder.setMessage(LocaleController.getString(R.string.ReportAlertReaction));

                TLRPC.Chat chat = getMessagesController().getChat(-reportReactionFromDialogId);
                CheckBoxCell[] cells = new CheckBoxCell[1];
                if (chat != null && ChatObject.canBlockUsers(chat)) {
                    LinearLayout linearLayout = new LinearLayout(getParentActivity());
                    linearLayout.setOrientation(LinearLayout.VERTICAL);
                    cells[0] = new CheckBoxCell(getParentActivity(), 1, resourcesProvider);
                    cells[0].setBackgroundDrawable(Theme.getSelectorDrawable(false));
                    cells[0].setText(LocaleController.getString(R.string.BanUser), "", true, false);
                    cells[0].setPadding(LocaleController.isRTL ? AndroidUtilities.dp(16) : AndroidUtilities.dp(8), 0, LocaleController.isRTL ? AndroidUtilities.dp(8) : AndroidUtilities.dp(16), 0);
                    linearLayout.addView(cells[0], LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT));
                    cells[0].setOnClickListener(v -> {
                        cells[0].setChecked(!cells[0].isChecked(), true);
                    });
                    builder.setView(linearLayout);
                }

                builder.setPositiveButton(LocaleController.getString(R.string.ReportChat), (dialog, which) -> {
                    TLRPC.TL_messages_reportReaction req = new TLRPC.TL_messages_reportReaction();
                    req.user_id = getMessagesController().getInputUser(userId);
                    req.peer = getMessagesController().getInputPeer(reportReactionFromDialogId);
                    req.id = reportReactionMessageId;
                    ConnectionsManager.getInstance(currentAccount).sendRequest(req, (response, error) -> {

                    });

                    if (cells[0] != null && cells[0].isChecked()) {
                        TLRPC.User user = getMessagesController().getUser(userId);
                        getMessagesController().deleteParticipantFromChat(-reportReactionFromDialogId, user);
                    }

                    reportReactionMessageId = 0;
                    updateListAnimated(false);
                    BulletinFactory.of(ProfileActivity.this).createReportSent(resourcesProvider).show();
                });
                builder.setNegativeButton(LocaleController.getString(R.string.Cancel), (dialog, which) -> {
                    dialog.dismiss();
                });
                AlertDialog dialog = builder.show();
                TextView button = (TextView) dialog.getButton(DialogInterface.BUTTON_POSITIVE);
                if (button != null) {
                    button.setTextColor(Theme.getColor(Theme.key_text_RedBold));
                }
            } else if (position == settingsKeyRow) {
                Bundle args = new Bundle();
                args.putInt("chat_id", DialogObject.getEncryptedChatId(dialogId));
                presentFragment(new IdenticonActivity(args));
            } else if (position == settingsTimerRow) {
                showDialog(AlertsCreator.createTTLAlert(getParentActivity(), currentEncryptedChat, resourcesProvider).create());
            } else if (position == notificationsRow) {
                onNotificationsClicked(false, x, y, view);

            } else if (position == unblockRow) {
                getMessagesController().unblockPeer(userId);
                if (BulletinFactory.canShowBulletin(ProfileActivity.this)) {
                    BulletinFactory.createBanBulletin(ProfileActivity.this, false).show();
                }
            } else if (position == addToGroupButtonRow) {
                try {
                    actionBar.getActionBarMenuOnItemClick().onItemClick(invite_to_group);
                } catch (Exception e) {
                    FileLog.e(e);
                }
            } else if (position == sendMessageRow) {
                onWriteButtonClick();
            } else if (position == reportRow) {
                ReportBottomSheet.openChat(ProfileActivity.this, getDialogId());
            } else if (position >= membersStartRow && position < membersEndRow) {
                TLRPC.ChatParticipant participant;
                if (!sortedUsers.isEmpty()) {
                    participant = chatInfo.participants.participants.get(sortedUsers.get(position - membersStartRow));
                } else {
                    participant = chatInfo.participants.participants.get(position - membersStartRow);
                }
                onMemberClick(participant, false, view);
            } else if (position == addMemberRow) {
                openAddMember();
            } else if (position == usernameRow) {
                processOnClickOrPress(position, view, x, y);
            } else if (position == locationRow) {
                if (chatInfo.location instanceof TLRPC.TL_channelLocation) {
                    LocationActivity fragment = new LocationActivity(LocationActivity.LOCATION_TYPE_GROUP_VIEW);
                    fragment.setChatLocation(chatId, (TLRPC.TL_channelLocation) chatInfo.location);
                    presentFragment(fragment);
                }
            } else if (position == joinRow) {
                onJoinClicked(false);
            } else if (position == subscribersRow) {
                Bundle args = new Bundle();
                args.putLong("chat_id", chatId);
                args.putInt("type", ChatUsersActivity.TYPE_USERS);
                ChatUsersActivity fragment = new ChatUsersActivity(args);
                fragment.setInfo(chatInfo);
                presentFragment(fragment);
            } else if (position == subscribersRequestsRow) {
                MemberRequestsActivity activity = new MemberRequestsActivity(chatId);
                presentFragment(activity);
            } else if (position == administratorsRow) {
                Bundle args = new Bundle();
                args.putLong("chat_id", chatId);
                args.putInt("type", ChatUsersActivity.TYPE_ADMIN);
                ChatUsersActivity fragment = new ChatUsersActivity(args);
                fragment.setInfo(chatInfo);
                presentFragment(fragment);
            } else if (position == settingsRow) {
                editItem.performClick();
            } else if (position == botStarsBalanceRow) {
                presentFragment(new BotStarsActivity(BotStarsActivity.TYPE_STARS, userId));
            } else if (position == botTonBalanceRow) {
                presentFragment(new BotStarsActivity(BotStarsActivity.TYPE_TON, userId));
            } else if (position == channelBalanceRow) {
                Bundle args = new Bundle();
                args.putLong("chat_id", chatId);
                args.putBoolean("start_from_monetization", true);
                presentFragment(new StatisticActivity(args));
            } else if (position == blockedUsersRow) {
                Bundle args = new Bundle();
                args.putLong("chat_id", chatId);
                args.putInt("type", ChatUsersActivity.TYPE_BANNED);
                ChatUsersActivity fragment = new ChatUsersActivity(args);
                fragment.setInfo(chatInfo);
                presentFragment(fragment);
            } else if (position == notificationRow) {
                presentFragment(new NotificationsSettingsActivity());
            } else if (position == privacyRow) {
                presentFragment(new PrivacySettingsActivity().setCurrentPassword(currentPassword));
            } else if (position == dataRow) {
                presentFragment(new DataSettingsActivity());
            } else if (position == chatRow) {
                presentFragment(new ThemeActivity(ThemeActivity.THEME_TYPE_BASIC));
            } else if (position == filtersRow) {
                presentFragment(new FiltersSetupActivity());
            } else if (position == stickersRow) {
                presentFragment(new StickersActivity(MediaDataController.TYPE_IMAGE, null));
            } else if (position == liteModeRow) {
                presentFragment(new LiteModeSettingsActivity());
            } else if (position == devicesRow) {
                presentFragment(new SessionsActivity(0));
            } else if (position == questionRow) {
                showDialog(AlertsCreator.createSupportAlert(ProfileActivity.this, resourcesProvider));
            } else if (position == faqRow) {
                Browser.openUrl(getParentActivity(), LocaleController.getString(R.string.TelegramFaqUrl));
            } else if (position == policyRow) {
                Browser.openUrl(getParentActivity(), LocaleController.getString(R.string.PrivacyPolicyUrl));
            } else if (position == sendLogsRow) {
                sendLogs(getParentActivity(), false);
            } else if (position == sendLastLogsRow) {
                sendLogs(getParentActivity(), true);
            } else if (position == clearLogsRow) {
                FileLog.cleanupLogs();
            } else if (position == switchBackendRow) {
                if (getParentActivity() == null) {
                    return;
                }
                AlertDialog.Builder builder1 = new AlertDialog.Builder(getParentActivity(), resourcesProvider);
                builder1.setMessage(LocaleController.getString(R.string.AreYouSure));
                builder1.setTitle(LocaleController.getString(R.string.AppName));
                builder1.setPositiveButton(LocaleController.getString(R.string.OK), (dialogInterface, i) -> {
                    SharedConfig.pushAuthKey = null;
                    SharedConfig.pushAuthKeyId = null;
                    SharedConfig.saveConfig();
                    getConnectionsManager().switchBackend(true);
                });
                builder1.setNegativeButton(LocaleController.getString(R.string.Cancel), null);
                showDialog(builder1.create());
            } else if (position == languageRow) {
                presentFragment(new LanguageSelectActivity());
            } else if (position == setUsernameRow) {
                presentFragment(new ChangeUsernameActivity());
            } else if (position == bioRow) {
                presentFragment(new UserInfoActivity());
            } else if (position == numberRow) {
                presentFragment(new ActionIntroActivity(ActionIntroActivity.ACTION_TYPE_CHANGE_PHONE_NUMBER));
            } else if (position == setAvatarRow) {
                onWriteButtonClick();
            } else if (position == premiumRow) {
                presentFragment(new PremiumPreviewFragment("settings"));
            } else if (position == starsRow) {
                presentFragment(new StarsIntroActivity());
            } else if (position == tonRow) {
                presentFragment(new TONIntroActivity());
            } else if (position == businessRow) {
                presentFragment(new PremiumPreviewFragment(PremiumPreviewFragment.FEATURES_BUSINESS, "settings"));
            } else if (position == premiumGiftingRow) {
                UserSelectorBottomSheet.open(0, BirthdayController.getInstance(currentAccount).getState());
            } else if (position == botPermissionLocation) {
                if (botLocation != null) {
                    botLocation.setGranted(!botLocation.granted(), () -> {
                        ((TextCell) view).setChecked(botLocation.granted());
                    });
                }
            } else if (position == botPermissionBiometry) {
                if (botBiometry != null) {
                    botBiometry.setGranted(!botBiometry.granted());
                    ((TextCell) view).setChecked(botBiometry.granted());
                }
            } else if (position == botPermissionEmojiStatus) {
                ((TextCell) view).setChecked(!((TextCell) view).isChecked());
                if (botPermissionEmojiStatusReqId > 0) {
                    getConnectionsManager().cancelRequest(botPermissionEmojiStatusReqId, true);
                }
                TL_bots.toggleUserEmojiStatusPermission req = new TL_bots.toggleUserEmojiStatusPermission();
                req.bot = getMessagesController().getInputUser(userId);
                req.enabled = ((TextCell) view).isChecked();
                if (userInfo != null) {
                    userInfo.bot_can_manage_emoji_status = req.enabled;
                }
                final int[] reqId = new int[1];
                reqId[0] = botPermissionEmojiStatusReqId = getConnectionsManager().sendRequest(req, (res, err) -> AndroidUtilities.runOnUIThread(() -> {
                    if (!(res instanceof TLRPC.TL_boolTrue)) {
                        BulletinFactory.of(ProfileActivity.this).showForError(err);
                    }
                    if (botPermissionEmojiStatusReqId == reqId[0]) {
                        botPermissionEmojiStatusReqId = 0;
                    }
                }));
            } else if (position == bizHoursRow) {
                hoursExpanded = !hoursExpanded;
                saveScrollPosition();
                view.requestLayout();
                listAdapter.notifyItemChanged(bizHoursRow);
                if (savedScrollPosition >= 0) {
                    layoutManager.scrollToPositionWithOffset(savedScrollPosition, savedScrollOffset - listView.getPaddingTop());
                }
            } else if (position == bizLocationRow) {
                openLocation(false);
            } else if (position == channelRow) {
                if (userInfo == null) return;
                Bundle args = new Bundle();
                args.putLong("chat_id", userInfo.personal_channel_id);
                presentFragment(new ChatActivity(args));
            } else if (position == birthdayRow) {
                if (birthdayEffect != null && birthdayEffect.start()) {
                    return;
                }
                if (editRow(view, position)) {
                    return;
                }
                TextDetailCell cell = (TextDetailCell) view;
                if (cell.hasImage()) {
                    onTextDetailCellImageClicked(cell.getImageView());
                }
            } else if (position == noteRow) {
                editNotes(view, position);
            } else {
                processOnClickOrPress(position, view, x, y);
            }
        });

        listView.setOnItemLongClickListener(new RecyclerListView.OnItemLongClickListener() {

            private int pressCount = 0;

            @Override
            public boolean onItemClick(View view, int position) {
                                if (position == chatIdRow) {
                    long id = currentChat != null ? -currentChat.id : userId;
                    android.content.ClipboardManager clipboard = (android.content.ClipboardManager) getParentActivity().getSystemService(android.content.Context.CLIPBOARD_SERVICE);
                    android.content.ClipData clip = android.content.ClipData.newPlainText("Chat ID", String.valueOf(id));
                    clipboard.setPrimaryClip(clip);
                    android.widget.Toast.makeText(getParentActivity(), "ID Copied: " + id, android.widget.Toast.LENGTH_SHORT).show();
                } else if (position == ownerRow) {
                    if (chatInfo != null && chatInfo.creator_id != 0) {
                        TLRPC.User owner = getMessagesController().getUser(chatInfo.creator_id);
                        String ownerInfo = "Owner ID: " + chatInfo.creator_id;
                        if (owner != null) {
                            ownerInfo += "\nName: " + UserObject.getName(owner);
                            if (owner.username != null) ownerInfo += "\n@" + owner.username;
                        }
                        final String finalInfo = ownerInfo;
                        new android.app.AlertDialog.Builder(getParentActivity())
                            .setTitle("Group Owner")
                            .setMessage(finalInfo)
                            .setPositiveButton("Copy ID", (d, w) -> {
                                android.content.ClipboardManager cb = (android.content.ClipboardManager) getParentActivity().getSystemService(android.content.Context.CLIPBOARD_SERVICE);
                                cb.setPrimaryClip(android.content.ClipData.newPlainText("Owner ID", String.valueOf(chatInfo.creator_id)));
                            })
                            .setNegativeButton("OK", null)
                            .show();
                    }
                } else if (position == versionRow) {
                    pressCount++;
                    if (pressCount >= 2 || BuildVars.DEBUG_PRIVATE_VERSION) {
                        AlertDialog.Builder builder = new AlertDialog.Builder(getParentActivity(), resourcesProvider);
                        builder.setTitle(getString(R.string.DebugMenu));
                        CharSequence[] items;
                        items = new CharSequence[]{
                                getString(R.string.DebugMenuImportContacts),
                                getString(R.string.DebugMenuReloadContacts),
                                getString(R.string.DebugMenuResetContacts),
                                getString(R.string.DebugMenuResetDialogs),
                                BuildVars.DEBUG_VERSION ? null : BuildVars.LOGS_ENABLED ? getString("DebugMenuDisableLogs", R.string.DebugMenuDisableLogs) : getString("DebugMenuEnableLogs", R.string.DebugMenuEnableLogs),
                                SharedConfig.inappCamera ? getString("DebugMenuDisableCamera", R.string.DebugMenuDisableCamera) : getString("DebugMenuEnableCamera", R.string.DebugMenuEnableCamera),
                                getString("DebugMenuClearMediaCache", R.string.DebugMenuClearMediaCache),
                                getString(R.string.DebugMenuCallSettings),
                                null,
                                BuildVars.DEBUG_PRIVATE_VERSION || ApplicationLoader.isStandaloneBuild() || ApplicationLoader.isBetaBuild() ? getString("DebugMenuCheckAppUpdate", R.string.DebugMenuCheckAppUpdate) : null,
                                getString("DebugMenuReadAllDialogs", R.string.DebugMenuReadAllDialogs),
                                BuildVars.DEBUG_PRIVATE_VERSION ? SharedConfig.disableVoiceAudioEffects ? "Enable voip audio effects" : "Disable voip audio effects" : null,
                                BuildVars.DEBUG_PRIVATE_VERSION ? "Clean app update" : null,
                                BuildVars.DEBUG_PRIVATE_VERSION ? "Reset suggestions" : null,
                                BuildVars.DEBUG_PRIVATE_VERSION ? getString(R.string.DebugMenuClearWebViewCache) : null,
                                getString(R.string.DebugMenuClearWebViewCookies),
                                getString(SharedConfig.debugWebView ? R.string.DebugMenuDisableWebViewDebug : R.string.DebugMenuEnableWebViewDebug),
                                AndroidUtilities.isTabletInternal() && BuildVars.DEBUG_PRIVATE_VERSION ? SharedConfig.forceDisableTabletMode ? "Enable tablet mode" : "Disable tablet mode" : null,
                                BuildVars.DEBUG_PRIVATE_VERSION ? getString(SharedConfig.isFloatingDebugActive ? R.string.FloatingDebugDisable : R.string.FloatingDebugEnable) : null,
                                BuildVars.DEBUG_PRIVATE_VERSION ? "Force remove premium suggestions" : null,
                                BuildVars.DEBUG_PRIVATE_VERSION ? "Share device info" : null,
                                BuildVars.DEBUG_PRIVATE_VERSION ? "Force performance class" : null,
                                BuildVars.DEBUG_PRIVATE_VERSION && !InstantCameraView.allowBigSizeCameraDebug() ? !SharedConfig.bigCameraForRound ? "Force big camera for round" : "Disable big camera for round" : null,
                                getString(DualCameraView.dualAvailableStatic(getContext()) ? "DebugMenuDualOff" : "DebugMenuDualOn"),
                                BuildVars.DEBUG_VERSION ? SharedConfig.useSurfaceInStories ? "back to TextureView in stories" : "use SurfaceView in stories" : null,
                                BuildVars.DEBUG_PRIVATE_VERSION ? SharedConfig.photoViewerBlur ? "do not blur in photoviewer" : "blur in photoviewer" : null,
                                !SharedConfig.payByInvoice ? "Enable Invoice Payment" : "Disable Invoice Payment",
                                BuildVars.DEBUG_PRIVATE_VERSION ? "Update Attach Bots" : null,
                                !SharedConfig.isUsingCamera2(currentAccount) ? "Use Camera 2 API" : "Use old Camera 1 API",
                                BuildVars.DEBUG_VERSION ? "Clear Mini Apps Permissions and Files" : null,
                                BuildVars.DEBUG_PRIVATE_VERSION ? "Clear all login tokens" : null,
                                SharedConfig.canBlurChat() && Build.VERSION.SDK_INT >= 31 ? SharedConfig.useNewBlur ? "back to cpu blur" : "use new gpu blur" : null,
                                SharedConfig.adaptableColorInBrowser ? "Disabled adaptive browser colors" : "Enable adaptive browser colors",
                                SharedConfig.debugVideoQualities ? "Disable video qualities debug" : "Enable video qualities debug",
                                Build.VERSION.SDK_INT >= Build.VERSION_CODES.P ? getString(SharedConfig.useSystemBoldFont ? R.string.DebugMenuDontUseSystemBoldFont : R.string.DebugMenuUseSystemBoldFont) : null,
                                "Reload app config",
                                !SharedConfig.forceForumTabs ? "Force Forum Tabs" : "Do Not Force Forum Tabs",
                            "Make Memory Dump",
                                BuildVars.DEBUG_PRIVATE_VERSION ? (SharedConfig.fastWallpaperDisabled ? "enable wallpaper shader" : "disable wallpaper shader") : null
                        };

                        builder.setItems(items, (dialog, which) -> {
                            if (which == 0) { // Import Contacts
                                getUserConfig().syncContacts = true;
                                getUserConfig().saveConfig(false);
                                getContactsController().forceImportContacts();
                            } else if (which == 1) { // Reload Contacts
                                getContactsController().loadContacts(false, 0);
                            } else if (which == 2) { // Reset Imported Contacts
                                getContactsController().resetImportedContacts();
                            } else if (which == 3) { // Reset Dialogs
                                getMessagesController().forceResetDialogs();
                            } else if (which == 4) { // Logs
                                BuildVars.LOGS_ENABLED = !BuildVars.LOGS_ENABLED;
                                SharedPreferences sharedPreferences = ApplicationLoader.applicationContext.getSharedPreferences("systemConfig", Context.MODE_PRIVATE);
                                sharedPreferences.edit().putBoolean("logsEnabled", BuildVars.LOGS_ENABLED).commit();
                                updateRowsIds();
                                listAdapter.notifyDataSetChanged();
                                if (BuildVars.LOGS_ENABLED) {
                                    FileLog.d("app start time = " + ApplicationLoader.startTime);
                                    try {
                                        FileLog.d("buildVersion = " + ApplicationLoader.applicationContext.getPackageManager().getPackageInfo(ApplicationLoader.applicationContext.getPackageName(), 0).versionCode);
                                    } catch (Exception e) {
                                        FileLog.e(e);
                                    }
                                }
                            } else if (which == 5) { // In-app camera
                                SharedConfig.toggleInappCamera();
                            } else if (which == 6) { // Clear sent media cache
                                getMessagesStorage().clearSentMedia();
                                SharedConfig.setNoSoundHintShowed(false);
                                SharedPreferences.Editor editor = MessagesController.getGlobalMainSettings().edit();
                                editor.remove("archivehint").remove("proximityhint").remove("archivehint_l").remove("searchpostsnew").remove("speedhint").remove("gifhint").remove("reminderhint").remove("soundHint").remove("themehint").remove("bganimationhint").remove("filterhint").remove("n_0").remove("storyprvhint").remove("storyhint").remove("storyhint2").remove("storydualhint").remove("storysvddualhint").remove("stories_camera").remove("dualcam").remove("dualmatrix").remove("dual_available").remove("archivehint").remove("askNotificationsAfter").remove("askNotificationsDuration").remove("viewoncehint").remove("voicepausehint").remove("taptostorysoundhint").remove("nothanos").remove("voiceoncehint").remove("savedhint").remove("savedsearchhint").remove("savedsearchtaghint").remove("newppsms").remove("monetizationadshint").remove("seekSpeedHintShowed").remove("unsupport_video/av01").remove("statusgiftpage").remove("multistorieshint").remove("trimvoicehint").remove("taptostoryhighlighthint").apply();
                                HintsController.resetAll();
                                MessagesController.getEmojiSettings(currentAccount).edit().remove("featured_hidden").remove("emoji_featured_hidden").commit();
                                SharedConfig.textSelectionHintShows = 0;
                                SharedConfig.lockRecordAudioVideoHint = 0;
                                SharedConfig.stickersReorderingHintUsed = false;
                                SharedConfig.forwardingOptionsHintShown = false;
                                SharedConfig.replyingOptionsHintShown = false;
                                SharedConfig.messageSeenHintCount = 3;
                                SharedConfig.emojiInteractionsHintCount = 3;
                                SharedConfig.dayNightThemeSwitchHintCount = 3;
                                SharedConfig.fastScrollHintCount = 3;
                                SharedConfig.stealthModeSendMessageConfirm = 2;
                                SharedConfig.updateStealthModeSendMessageConfirm(2);
                                SharedConfig.setStoriesReactionsLongPressHintUsed(false);
                                SharedConfig.setStoriesIntroShown(false);
                                SharedConfig.setMultipleReactionsPromoShowed(false);
                                ChatThemeController.getInstance(currentAccount).clearCache();
                                getNotificationCenter().postNotificationName(NotificationCenter.newSuggestionsAvailable);
                                RestrictedLanguagesSelectActivity.cleanup();
                                PersistColorPalette.getInstance(currentAccount).cleanup();
                                SharedPreferences prefs = getMessagesController().getMainSettings();
                                editor = prefs.edit();
                                editor.remove("peerColors").remove("profilePeerColors").remove("boostingappearance").remove("bizbothint").remove("movecaptionhint");
                                for (String key : prefs.getAll().keySet()) {
                                    if (key.contains("show_gift_for_") || key.contains("bdayhint_") || key.contains("bdayanim_") || key.startsWith("ask_paid_message_") || key.startsWith("topicssidetabs")) {
                                        editor.remove(key);
                                    }
                                }
                                editor.apply();
                                editor = MessagesController.getNotificationsSettings(currentAccount).edit();
                                for (String key : MessagesController.getNotificationsSettings(currentAccount).getAll().keySet()) {
                                    if (key.startsWith("dialog_bar_botver")) {
                                        editor.remove(key);
                                    }
                                }
                                editor.apply();
                            } else if (which == 7) { // Call settings
                                VoIPHelper.showCallDebugSettings(getParentActivity());
                            } else if (which == 8) { // ?
                                SharedConfig.toggleRoundCamera16to9();
                            } else if (which == 9) { // Check app update
                                ((LaunchActivity) getParentActivity()).checkAppUpdate(true, null);
                            } else if (which == 10) { // Read all chats
                                getMessagesStorage().readAllDialogs(-1);
                            } else if (which == 11) { // Voip audio effects
                                SharedConfig.toggleDisableVoiceAudioEffects();
                            } else if (which == 12) { // Clean app update
                                SharedConfig.pendingAppUpdate = null;
                                SharedConfig.saveConfig();
                                NotificationCenter.getGlobalInstance().postNotificationName(NotificationCenter.appUpdateAvailable);
                            } else if (which == 13) { // Reset suggestions
                                Set<String> suggestions = getMessagesController().pendingSuggestions;
                                suggestions.add("VALIDATE_PHONE_NUMBER");
                                suggestions.add("VALIDATE_PASSWORD");
                                getNotificationCenter().postNotificationName(NotificationCenter.newSuggestionsAvailable);
                            } else if (which == 14) { // WebView Cache
                                ApplicationLoader.applicationContext.deleteDatabase("webview.db");
                                ApplicationLoader.applicationContext.deleteDatabase("webviewCache.db");
                                WebStorage.getInstance().deleteAllData();
                                try {
                                    WebView webView = new WebView(ApplicationLoader.applicationContext);
                                    webView.clearHistory();
                                    webView.destroy();
                                } catch (Exception e) {
                                }
                            } else if (which == 15) {
                                CookieManager cookieManager = CookieManager.getInstance();
                                cookieManager.removeAllCookies(null);
                                cookieManager.flush();
                            } else if (which == 16) { // WebView debug
                                SharedConfig.toggleDebugWebView();
                                Toast.makeText(getParentActivity(), getString(SharedConfig.debugWebView ? R.string.DebugMenuWebViewDebugEnabled : R.string.DebugMenuWebViewDebugDisabled), Toast.LENGTH_SHORT).show();
                            } else if (which == 17) { // Tablet mode
                                SharedConfig.toggleForceDisableTabletMode();

                                Activity activity = AndroidUtilities.findActivity(context);
                                final PackageManager pm = activity.getPackageManager();
                                final Intent intent = pm.getLaunchIntentForPackage(activity.getPackageName());
                                activity.finishAffinity(); // Finishes all activities.
                                activity.startActivity(intent);    // Start the launch activity
                                System.exit(0);
                            } else if (which == 18) {
                                FloatingDebugController.setActive((LaunchActivity) getParentActivity(), !FloatingDebugController.isActive());
                            } else if (which == 19) {
                                getMessagesController().loadAppConfig();
                                TLRPC.TL_help_dismissSuggestion req = new TLRPC.TL_help_dismissSuggestion();
                                req.suggestion = "VALIDATE_PHONE_NUMBER";
                                req.peer = new TLRPC.TL_inputPeerEmpty();
                                getConnectionsManager().sendRequest(req, (response, error) -> {
                                    TLRPC.TL_help_dismissSuggestion req2 = new TLRPC.TL_help_dismissSuggestion();
                                    req2.suggestion = "VALIDATE_PASSWORD";
                                    req2.peer = new TLRPC.TL_inputPeerEmpty();
                                    getConnectionsManager().sendRequest(req2, (res2, err2) -> {
                                        getMessagesController().loadAppConfig();
                                    });
                                });
                            } else if (which == 20) {
                                int cpuCount = ConnectionsManager.CPU_COUNT;
                                int memoryClass = ((ActivityManager) ApplicationLoader.applicationContext.getSystemService(Context.ACTIVITY_SERVICE)).getMemoryClass();
                                long minFreqSum = 0, minFreqCount = 0;
                                long maxFreqSum = 0, maxFreqCount = 0;
                                long curFreqSum = 0, curFreqCount = 0;
                                long capacitySum = 0, capacityCount = 0;
                                StringBuilder cpusInfo = new StringBuilder();
                                for (int i = 0; i < cpuCount; i++) {
                                    Long minFreq = AndroidUtilities.getSysInfoLong("/sys/devices/system/cpu/cpu" + i + "/cpufreq/cpuinfo_min_freq");
                                    Long curFreq = AndroidUtilities.getSysInfoLong("/sys/devices/system/cpu/cpu" + i + "/cpufreq/cpuinfo_cur_freq");
                                    Long maxFreq = AndroidUtilities.getSysInfoLong("/sys/devices/system/cpu/cpu" + i + "/cpufreq/cpuinfo_max_freq");
                                    Long capacity = AndroidUtilities.getSysInfoLong("/sys/devices/system/cpu/cpu" + i + "/cpu_capacity");
                                    cpusInfo.append("#").append(i).append(" ");
                                    if (minFreq != null) {
                                        cpusInfo.append("min=").append(minFreq / 1000L).append(" ");
                                        minFreqSum += (minFreq / 1000L);
                                        minFreqCount++;
                                    }
                                    if (curFreq != null) {
                                        cpusInfo.append("cur=").append(curFreq / 1000L).append(" ");
                                        curFreqSum += (curFreq / 1000L);
                                        curFreqCount++;
                                    }
                                    if (maxFreq != null) {
                                        cpusInfo.append("max=").append(maxFreq / 1000L).append(" ");
                                        maxFreqSum += (maxFreq / 1000L);
                                        maxFreqCount++;
                                    }
                                    if (capacity != null) {
                                        cpusInfo.append("cpc=").append(capacity).append(" ");
                                        capacitySum += capacity;
                                        capacityCount++;
                                    }
                                    cpusInfo.append("\n");
                                }
                                StringBuilder info = new StringBuilder();
                                info.append(Build.MANUFACTURER).append(", ").append(Build.MODEL).append(" (").append(Build.PRODUCT).append(", ").append(Build.DEVICE).append(") ").append(" (android ").append(Build.VERSION.SDK_INT).append(")\n");
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                                    info.append("SoC: ").append(Build.SOC_MANUFACTURER).append(", ").append(Build.SOC_MODEL).append("\n");
                                }
                                String gpuModel = AndroidUtilities.getSysInfoString("/sys/kernel/gpu/gpu_model");
                                if (gpuModel != null) {
                                    info.append("GPU: ").append(gpuModel);
                                    Long minClock = AndroidUtilities.getSysInfoLong("/sys/kernel/gpu/gpu_min_clock");
                                    Long mminClock = AndroidUtilities.getSysInfoLong("/sys/kernel/gpu/gpu_mm_min_clock");
                                    Long maxClock = AndroidUtilities.getSysInfoLong("/sys/kernel/gpu/gpu_max_clock");
                                    if (minClock != null) {
                                        info.append(", min=").append(minClock / 1000L);
                                    }
                                    if (mminClock != null) {
                                        info.append(", mmin=").append(mminClock / 1000L);
                                    }
                                    if (maxClock != null) {
                                        info.append(", max=").append(maxClock / 1000L);
                                    }
                                    info.append("\n");
                                }
                                ConfigurationInfo configurationInfo = ((ActivityManager) ApplicationLoader.applicationContext.getSystemService(Context.ACTIVITY_SERVICE)).getDeviceConfigurationInfo();
                                info.append("GLES Version: ").append(configurationInfo.getGlEsVersion()).append("\n");
                                info.append("Memory: class=").append(AndroidUtilities.formatFileSize(memoryClass * 1024L * 1024L));
                                ActivityManager.MemoryInfo memoryInfo = new ActivityManager.MemoryInfo();
                                ((ActivityManager) ApplicationLoader.applicationContext.getSystemService(Context.ACTIVITY_SERVICE)).getMemoryInfo(memoryInfo);
                                info.append(", total=").append(AndroidUtilities.formatFileSize(memoryInfo.totalMem));
                                info.append(", avail=").append(AndroidUtilities.formatFileSize(memoryInfo.availMem));
                                info.append(", low?=").append(memoryInfo.lowMemory);
                                info.append(" (threshold=").append(AndroidUtilities.formatFileSize(memoryInfo.threshold)).append(")");
                                info.append("\n");
                                info.append("Current class: ").append(SharedConfig.performanceClassName(SharedConfig.getDevicePerformanceClass())).append(", measured: ").append(SharedConfig.performanceClassName(SharedConfig.measureDevicePerformanceClass()));
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                                    info.append(", suggest=").append(Build.VERSION.MEDIA_PERFORMANCE_CLASS);
                                }
                                info.append("\n");
                                info.append(cpuCount).append(" CPUs");
                                if (minFreqCount > 0) {
                                    info.append(", avgMinFreq=").append(minFreqSum / minFreqCount);
                                }
                                if (curFreqCount > 0) {
                                    info.append(", avgCurFreq=").append(curFreqSum / curFreqCount);
                                }
                                if (maxFreqCount > 0) {
                                    info.append(", avgMaxFreq=").append(maxFreqSum / maxFreqCount);
                                }
                                if (capacityCount > 0) {
                                    info.append(", avgCapacity=").append(capacitySum / capacityCount);
                                }
                                info.append("\n").append(cpusInfo);

                                listCodecs("video/avc", info);
                                listCodecs("video/hevc", info);
                                listCodecs("video/x-vnd.on2.vp8", info);
                                listCodecs("video/x-vnd.on2.vp9", info);

                                showDialog(new ShareAlert(getParentActivity(), null, info.toString(), false, null, false) {
                                    @Override
                                    protected void onSend(LongSparseArray<TLRPC.Dialog> dids, int count, TLRPC.TL_forumTopic topic, boolean showToast) {
                                        if (!showToast) return;
                                        AndroidUtilities.runOnUIThread(() -> {
                                            BulletinFactory.createInviteSentBulletin(getParentActivity(), contentView, dids.size(), dids.size() == 1 ? dids.valueAt(0).id : 0, count, getThemedColor(Theme.key_undo_background), getThemedColor(Theme.key_undo_infoColor)).show();
                                        }, 250);
                                    }
                                });
                            } else if (which == 21) {
                                AlertDialog.Builder builder2 = new AlertDialog.Builder(getParentActivity(), resourcesProvider);
                                builder2.setTitle("Force performance class");
                                int currentClass = SharedConfig.getDevicePerformanceClass();
                                int trueClass = SharedConfig.measureDevicePerformanceClass();
                                builder2.setItems(new CharSequence[]{
                                        AndroidUtilities.replaceTags((currentClass == SharedConfig.PERFORMANCE_CLASS_HIGH ? "**HIGH**" : "HIGH") + (trueClass == SharedConfig.PERFORMANCE_CLASS_HIGH ? " (measured)" : "")),
                                        AndroidUtilities.replaceTags((currentClass == SharedConfig.PERFORMANCE_CLASS_AVERAGE ? "**AVERAGE**" : "AVERAGE") + (trueClass == SharedConfig.PERFORMANCE_CLASS_AVERAGE ? " (measured)" : "")),
                                        AndroidUtilities.replaceTags((currentClass == SharedConfig.PERFORMANCE_CLASS_LOW ? "**LOW**" : "LOW") + (trueClass == SharedConfig.PERFORMANCE_CLASS_LOW ? " (measured)" : ""))
                                }, (dialog2, which2) -> {
                                    int newClass = 2 - which2;
                                    if (newClass == trueClass) {
                                        SharedConfig.overrideDevicePerformanceClass(-1);
                                    } else {
                                        SharedConfig.overrideDevicePerformanceClass(newClass);
                                    }
                                });
                                builder2.setNegativeButton(getString("Cancel", R.string.Cancel), null);
                                builder2.show();
                            } else if (which == 22) {
                                SharedConfig.toggleRoundCamera();
                            } else if (which == 23) {
                                boolean enabled = DualCameraView.dualAvailableStatic(getContext());
                                MessagesController.getGlobalMainSettings().edit().putBoolean("dual_available", !enabled).apply();
                                try {
                                    Toast.makeText(getParentActivity(), getString(!enabled ? R.string.DebugMenuDualOnToast : R.string.DebugMenuDualOffToast), Toast.LENGTH_SHORT).show();
                                } catch (Exception e) {
                                }
                            } else if (which == 24) {
                                SharedConfig.toggleSurfaceInStories();
                                for (int i = 0; i < getParentLayout().getFragmentStack().size(); i++) {
                                    getParentLayout().getFragmentStack().get(i).clearSheets();
                                }
                            } else if (which == 25) {
                                SharedConfig.togglePhotoViewerBlur();
                            } else if (which == 26) {
                                SharedConfig.togglePaymentByInvoice();
                            } else if (which == 27) {
                                getMediaDataController().loadAttachMenuBots(false, true);
                            } else if (which == 28) {
                                SharedConfig.toggleUseCamera2(currentAccount);
                            } else if (which == 29) {
                                BotBiometry.clear();
                                BotLocation.clear();
                                BotDownloads.clear();
                                SetupEmojiStatusSheet.clear();
                            } else if (which == 30) {
                                AuthTokensHelper.clearLogInTokens();
                            } else if (which == 31) {
                                SharedConfig.toggleUseNewBlur();
                            } else if (which == 32) {
                                SharedConfig.toggleBrowserAdaptableColors();
                            } else if (which == 33) {
                                SharedConfig.toggleDebugVideoQualities();
                            } else if (which == 34) {
                                SharedConfig.toggleUseSystemBoldFont();
                            } else if (which == 35) {
                                MessagesController.getInstance(currentAccount).loadAppConfig(true);
                            } else if (which == 36) {
                                SharedConfig.toggleForceForumTabs();
                            } else if (which == 37) {
                                FileLog.getInstance().dumpMemory(true);
                            } else if (which == 38) {
                                SharedConfig.toggleFastWallpaperDisabled();
                            }
                        });
                        builder.setNegativeButton(getString("Cancel", R.string.Cancel), null);
                        showDialog(builder.create());
                    } else {
                        try {
                            Toast.makeText(getParentActivity(), getString("DebugMenuLongPress", R.string.DebugMenuLongPress), Toast.LENGTH_SHORT).show();
                        } catch (Exception e) {
                            FileLog.e(e);
                        }
                    }
                    return true;
                } else if (position >= membersStartRow && position < membersEndRow) {
                    final TLRPC.ChatParticipant participant;
                    if (!sortedUsers.isEmpty()) {
                        participant = visibleChatParticipants.get(sortedUsers.get(position - membersStartRow));
                    } else {
                        participant = visibleChatParticipants.get(position - membersStartRow);
                    }
                    return onMemberClick(participant, true, view);
                } else if (position == birthdayRow) {
                    if (editRow(view, position)) return true;
                    if (userInfo == null) return false;
                    try {
                        AndroidUtilities.addToClipboard(UserInfoActivity.birthdayString(userInfo.birthday));
                        BulletinFactory.of(ProfileActivity.this).createCopyBulletin(getString(R.string.BirthdayCopied)).show();
                    } catch (Exception e) {
                        FileLog.e(e);
                    }
                    return true;
                } else if (position == noteRow) {
                    editNotes(view, position);
                    return true;
                } else {
                    if (editRow(view, position)) return true;
                    return processOnClickOrPress(position, view, view.getWidth() / 2f, (int) (view.getHeight() * .75f));
                }
            }
        });

        if (openSimilar || openGifts || openCommonChats) {
            updateRowsIds();
            scrollToSharedMedia();
            savedScrollToSharedMedia = true;
            savedScrollPosition = sharedMediaRow;
            savedScrollOffset = 0;
        }

        if (searchItem != null) {
            searchListView = new RecyclerListView(context);
            searchListView.setVerticalScrollBarEnabled(false);
            searchListView.setLayoutManager(new LinearLayoutManager(context, LinearLayoutManager.VERTICAL, false));
            searchListView.setGlowColor(getThemedColor(Theme.key_avatar_backgroundActionBarBlue));
            searchListView.setAdapter(searchAdapter);
            searchListView.setItemAnimator(null);
            searchListView.setVisibility(View.GONE);
            searchListView.setLayoutAnimation(null);
            searchListView.setBackgroundColor(getThemedColor(Theme.key_windowBackgroundWhite));
            frameLayout.addView(searchListView, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.MATCH_PARENT, Gravity.TOP | Gravity.LEFT));
            searchListView.setOnItemClickListener((view, position) -> {
                if (position < 0) {
                    return;
                }
                Object object = numberRow;
                boolean add = true;
                if (searchAdapter.searchWas) {
                    if (position < searchAdapter.searchResults.size()) {
                        object = searchAdapter.searchResults.get(position);
                    } else {
                        position -= searchAdapter.searchResults.size() + 1;
                        if (position >= 0 && position < searchAdapter.faqSearchResults.size()) {
                            object = searchAdapter.faqSearchResults.get(position);
                        }
                    }
                } else {
                    if (!searchAdapter.recentSearches.isEmpty()) {
                        position--;
                    }
                    if (position >= 0 && position < searchAdapter.recentSearches.size()) {
                        object = searchAdapter.recentSearches.get(position);
                    } else {
                        position -= searchAdapter.recentSearches.size() + 1;
                        if (position >= 0 && position < searchAdapter.faqSearchArray.size()) {
                            object = searchAdapter.faqSearchArray.get(position);
                            add = false;
                        }
                    }
                }
                if (object instanceof SearchAdapter.SearchResult) {
                    SearchAdapter.SearchResult result = (SearchAdapter.SearchResult) object;
                    result.open(getParentLayout());
                } else if (object instanceof MessagesController.FaqSearchResult) {
                    MessagesController.FaqSearchResult result = (MessagesController.FaqSearchResult) object;
                    NotificationCenter.getInstance(currentAccount).postNotificationName(NotificationCenter.openArticle, searchAdapter.faqWebPage, result.url);
                }
                if (add && object != null) {
                    searchAdapter.addRecent(object);
                }
            });
            searchListView.setOnItemLongClickListener((view, position) -> {
                if (searchAdapter.isSearchWas() || searchAdapter.recentSearches.isEmpty()) {
                    return false;
                }
                AlertDialog.Builder builder = new AlertDialog.Builder(getParentActivity(), resourcesProvider);
                builder.setTitle(LocaleController.getString(R.string.ClearSearchAlertTitle));
                builder.setMessage(LocaleController.getString(R.string.ClearSearchAlert));
                builder.setPositiveButton(LocaleController.getString(R.string.ClearButton), (dialogInterface, i) -> searchAdapter.clearRecent());
                builder.setNegativeButton(LocaleController.getString(R.string.Cancel), null);
                AlertDialog dialog = builder.create();
                showDialog(dialog);
                TextView button = (TextView) dialog.getButton(DialogInterface.BUTTON_POSITIVE);
                if (button != null) {
                    button.setTextColor(Theme.getColor(Theme.key_text_RedBold));
                }
                return true;
            });
            searchListView.setOnScrollListener(new RecyclerView.OnScrollListener() {
                @Override
                public void onScrollStateChanged(RecyclerView recyclerView, int newState) {
                    if (newState == RecyclerView.SCROLL_STATE_DRAGGING) {
                        AndroidUtilities.hideKeyboard(getParentActivity().getCurrentFocus());
                    }
                }
            });
            searchListView.setAnimateEmptyView(true, RecyclerListView.EMPTY_VIEW_ANIMATION_TYPE_ALPHA_SCALE);

            emptyView = new StickerEmptyView(context, null, 1);
            emptyView.setAnimateLayoutChange(true);
            emptyView.subtitle.setVisibility(View.GONE);
            emptyView.setVisibility(View.GONE);
            frameLayout.addView(emptyView);

            searchAdapter.loadFaqWebPage();
        }

        if (banFromGroup != 0) {
            TLRPC.Chat chat = getMessagesController().getChat(banFromGroup);
            if (currentChannelParticipant == null) {
                TLRPC.TL_channels_getParticipant req = new TLRPC.TL_channels_getParticipant();
                req.channel = MessagesController.getInputChannel(chat);
                req.participant = getMessagesController().getInputPeer(userId);
                getConnectionsManager().sendRequest(req, (response, error) -> {
                    if (response != null) {
                        AndroidUtilities.runOnUIThread(() -> currentChannelParticipant = ((TLRPC.TL_channels_channelParticipant) response).participant);
                    }
                });
            }
            FrameLayout frameLayout1 = new FrameLayout(context) {
                @Override
                protected void onDraw(Canvas canvas) {
                    int bottom = Theme.chat_composeShadowDrawable.getIntrinsicHeight();
                    Theme.chat_composeShadowDrawable.setBounds(0, 0, getMeasuredWidth(), bottom);
                    Theme.chat_composeShadowDrawable.draw(canvas);
                    canvas.drawRect(0, bottom, getMeasuredWidth(), getMeasuredHeight(), Theme.chat_composeBackgroundPaint);
                }
            };
            frameLayout1.setWillNotDraw(false);
            frameLayout1.setPadding(0, 0, 0, AndroidUtilities.navigationBarHeight);

            frameLayout.addView(frameLayout1, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, 51 + AndroidUtilities.navigationBarHeight / AndroidUtilities.density, Gravity.LEFT | Gravity.BOTTOM));
            frameLayout1.setOnClickListener(v -> {
                ChatRightsEditActivity fragment = new ChatRightsEditActivity(userId, banFromGroup, null, chat.default_banned_rights, currentChannelParticipant != null ? currentChannelParticipant.banned_rights : null, "", ChatRightsEditActivity.TYPE_BANNED, true, false, null);
                fragment.setDelegate(new ChatRightsEditActivity.ChatRightsEditActivityDelegate() {
                    @Override
                    public void didSetRights(int rights, TLRPC.TL_chatAdminRights rightsAdmin, TLRPC.TL_chatBannedRights rightsBanned, String rank) {
                        removeSelfFromStack();
                        TLRPC.User user = getMessagesController().getUser(userId);
                        if (user != null && chat != null && userId != 0 && fragment != null && fragment.banning && fragment.getParentLayout() != null) {
                            for (BaseFragment fragment : fragment.getParentLayout().getFragmentStack()) {
                                if (fragment instanceof ChannelAdminLogActivity) {
                                    ((ChannelAdminLogActivity) fragment).reloadLastMessages();
                                    AndroidUtilities.runOnUIThread(() -> {
                                        BulletinFactory.createRemoveFromChatBulletin(fragment, user, chat.title).show();
                                    });
                                    return;
                                }
                            }
                        }
                    }

                    @Override
                    public void didChangeOwner(TLRPC.User user) {
                        undoView.showWithAction(-chatId, currentChat.megagroup ? UndoView.ACTION_OWNER_TRANSFERED_GROUP : UndoView.ACTION_OWNER_TRANSFERED_CHANNEL, user);
                    }
                });
                presentFragment(fragment);
            });

            TextView textView = new TextView(context);
            textView.setTextColor(getThemedColor(Theme.key_text_RedRegular));
            textView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 15);
            textView.setGravity(Gravity.CENTER);
            textView.setTypeface(AndroidUtilities.bold());
            textView.setText(LocaleController.getString(R.string.BanFromTheGroupNoCaps));
            frameLayout1.addView(textView, LayoutHelper.createFrame(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT, Gravity.CENTER, 0, 1, 0, 0));

            listView.setPadding(0, getHeaderExtraHeight(), 0, AndroidUtilities.dp(48));
            listView.setBottomGlowOffset(AndroidUtilities.dp(48));
        } else {
            listView.setPadding(0, getHeaderExtraHeight(), 0, 0);
        }

        topView = new TopView(context);
        topView.setBackgroundColorId(peerColor, false);
        topView.setBackgroundColor(getThemedColor(Theme.key_windowBackgroundGray));
        frameLayout.addView(topView);
        contentView.blurBehindViews.add(topView);

        animatedStatusView = new AnimatedStatusView(context, 20, 60);
        animatedStatusView.setPivotX(dp(30));
        animatedStatusView.setPivotY(dp(30));

        avatarContainer = new FrameLayout(context) {
            @Override
            public void setScaleX(float scaleX) {
                super.setScaleX(scaleX);
                updateGooey();
            }

            @Override
            protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
                super.onMeasure(widthMeasureSpec, heightMeasureSpec);
                updateGooey();
            }

            @Override
            public void setLayoutParams(ViewGroup.LayoutParams params) {
                super.setLayoutParams(params);
            }

            @Override
            protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
                super.onLayout(changed, left, top, right, bottom);
            }
        };
        avatarContainer2 = new FrameLayout(context) {

            CanvasButton canvasButton;

            @Override
            protected void dispatchDraw(Canvas canvas) {
                super.dispatchDraw(canvas);
                if (transitionOnlineText != null) {
                    canvas.save();
                    canvas.translate(onlineTextView[0].getX(), onlineTextView[0].getY());
                    canvas.saveLayerAlpha(0, 0, transitionOnlineText.getMeasuredWidth(), transitionOnlineText.getMeasuredHeight(), (int) (255 * (1f - avatarAnimationProgress)), Canvas.ALL_SAVE_FLAG);
                    transitionOnlineText.draw(canvas);
                    canvas.restore();
                    canvas.restore();
                    invalidate();
                }
                if (hasFallbackPhoto && photoDescriptionProgress != 0 && customAvatarProgress != 1f) {
                    float cy = onlineTextView[1].getY() + onlineTextView[1].getMeasuredHeight() / 2f;
                    float size = dp(22);
                    float x = dp(28) - customPhotoOffset + onlineTextView[1].getX() - size - getRatingViewTranslationXOffset();

                    fallbackImage.setImageCoords(x, cy - size / 2f, size, size);
                    fallbackImage.setAlpha(photoDescriptionProgress);
                    canvas.save();
                    float s = photoDescriptionProgress;
                    canvas.scale(s, s, fallbackImage.getCenterX(), fallbackImage.getCenterY());
                    fallbackImage.draw(canvas);
                    canvas.restore();

                    if (customAvatarProgress == 0) {
                        if (canvasButton == null) {
                            canvasButton = new CanvasButton(this);
                            canvasButton.setDelegate(() -> {
                                if (customAvatarProgress != 1f) {
                                    avatarsViewPager.scrollToLastItem();
                                }
                            });
                        }
                        float cbX = x;
                        float cbW = AndroidUtilities.dp(28) * (1f - customAvatarProgress);

                        float cbW1 = onlineTextView[2].getTextWidth();
                        float cbW2 = 0;
                        if (ratingView != null) {
                            cbW2 = (dp(24) + cbW1 + dp(4)) * ratingView.getVisibilityFactor();
                        }
                        cbW += Math.max(cbW1, cbW2);

                        AndroidUtilities.rectTmp.set(cbX - AndroidUtilities.dp(4), cy - AndroidUtilities.dp(14), x + cbW + AndroidUtilities.dp(4), cy + AndroidUtilities.dp(14));
                        canvasButton.setRect(AndroidUtilities.rectTmp);
                        canvasButton.setRounded(true);
                        canvasButton.setColor(Color.TRANSPARENT, ColorUtils.setAlphaComponent(Color.WHITE, 50));
                        canvasButton.draw(canvas);
                    } else {
                        if (canvasButton != null) {
                            canvasButton.cancelRipple();
                        }
                    }
                }
            }

            @Override
            public boolean onInterceptTouchEvent(MotionEvent ev) {
                return (customAvatarProgress == 0 && canvasButton != null && canvasButton.checkTouchEvent(ev)) || super.onInterceptTouchEvent(ev);
            }

            @Override
            public boolean onTouchEvent(MotionEvent event) {
                return (customAvatarProgress == 0 && canvasButton != null && canvasButton.checkTouchEvent(event)) || super.onTouchEvent(event);
            }

            @Override
            protected void onAttachedToWindow() {
                super.onAttachedToWindow();
                fallbackImage.onAttachedToWindow();
            }

            @Override
            protected void onDetachedFromWindow() {
                super.onDetachedFromWindow();
                fallbackImage.onDetachedFromWindow();
            }

            @Override
            protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
                super.onLayout(changed, left, top, right, bottom);
                updateCollectibleHint();
            }
        };
        fallbackImage = new ImageReceiver(avatarContainer2);
        fallbackImage.setRoundRadius(AndroidUtilities.dp(11));
        AndroidUtilities.updateViewVisibilityAnimated(avatarContainer2, true, 1f, false);
        frameLayout.addView(avatarContainer2, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.MATCH_PARENT, Gravity.START, 0, 0, 0, 0));
        avatarContainer.setPivotX(0);
        avatarContainer.setPivotY(0);
        avatarGooey = new ProfileGooeyView(context);
        avatarGooey.addView(avatarContainer, LayoutHelper.createFrame(100, 100, Gravity.TOP | Gravity.LEFT, 64, 0, 0, 0));
        avatarContainer2.addView(avatarGooey, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.MATCH_PARENT));
//        avatarContainer2.addView(avatarContainer, LayoutHelper.createFrame(100, 100, Gravity.TOP | Gravity.LEFT, 64, 0, 0, 0));
        avatarImage = new AvatarImageView(context) {
            @Override
            public void onInitializeAccessibilityNodeInfo(AccessibilityNodeInfo info) {
                super.onInitializeAccessibilityNodeInfo(info);
                if (getImageReceiver().hasNotThumb()) {
                    info.setText(LocaleController.getString(R.string.AccDescrProfilePicture));
                    info.addAction(new AccessibilityNodeInfo.AccessibilityAction(AccessibilityNodeInfo.ACTION_CLICK, LocaleController.getString(R.string.Open)));
                    info.addAction(new AccessibilityNodeInfo.AccessibilityAction(AccessibilityNodeInfo.ACTION_LONG_CLICK, LocaleController.getString(R.string.AccDescrOpenInPhotoViewer)));
                } else {
                    info.setVisibleToUser(false);
                }
            }

            @Override
            protected void dispatchDraw(Canvas canvas) {
                super.dispatchDraw(canvas);
                if (animatedEmojiDrawable != null && animatedEmojiDrawable.getImageReceiver() != null) {
                    animatedEmojiDrawable.getImageReceiver().startAnimation();
                }
            }
        };
        avatarImage.createBlurEffect(getActionsExtraHeight());
        avatarImage.getImageReceiver().setAllowDecodeSingleFrame(true);
        avatarImage.setRoundRadiusForExpand(getSmallAvatarRoundRadius());
        avatarImage.setPivotX(0);
        avatarImage.setPivotY(0);
        avatarContainer.addView(avatarImage, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.MATCH_PARENT));
        avatarImage.setOnClickListener(v -> {
            if (avatarBig != null) {
                return;
            }
            if (isTopic && !getMessagesController().premiumFeaturesBlocked()) {
                ArrayList<TLRPC.TL_forumTopic> topics = getMessagesController().getTopicsController().getTopics(chatId);
                if (topics != null) {
                    TLRPC.TL_forumTopic currentTopic = null;
                    for (int i = 0; currentTopic == null && i < topics.size(); ++i) {
                        TLRPC.TL_forumTopic topic = topics.get(i);
                        if (topic != null && topic.id == topicId) {
                            currentTopic = topic;
                        }
                    }
                    if (currentTopic != null && currentTopic.icon_emoji_id != 0) {
                        long documentId = currentTopic.icon_emoji_id;
                        TLRPC.Document document = AnimatedEmojiDrawable.findDocument(currentAccount, documentId);
                        if (document == null) {
                            return;
                        }
                        Bulletin bulletin = BulletinFactory.of(ProfileActivity.this).createContainsEmojiBulletin(document, BulletinFactory.CONTAINS_EMOJI_IN_TOPIC, set -> {
                            ArrayList<TLRPC.InputStickerSet> inputSets = new ArrayList<>(1);
                            inputSets.add(set);
                            EmojiPacksAlert alert = new EmojiPacksAlert(ProfileActivity.this, getParentActivity(), resourcesProvider, inputSets);
                            showDialog(alert);
                        });
                        if (bulletin != null) {
                            bulletin.show();
                        }
                    }
                }
                return;
            }
            if (expandAvatar()) {
                return;
            }
            openAvatar();
        });
        avatarImage.setHasStories(needInsetForStories());
        avatarImage.setOnLongClickListener(v -> {
            if (avatarBig != null || isTopic) {
                return false;
            }
            openAvatar();
            return false;
        });

        avatarProgressView = new RadialProgressView(context) {
            private Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);

            {
                paint.setColor(0x55000000);
            }

            @Override
            protected void onDraw(Canvas canvas) {
                if (avatarImage != null && avatarImage.getImageReceiver().hasNotThumb()) {
                    paint.setAlpha((int) (0x55 * avatarImage.getImageReceiver().getCurrentAlpha()));
                    canvas.drawCircle(getMeasuredWidth() / 2.0f, getMeasuredHeight() / 2.0f, getMeasuredWidth() / 2.0f, paint);
                }
                super.onDraw(canvas);
            }
        };
        avatarProgressView.setSize(AndroidUtilities.dp(26));
        avatarProgressView.setProgressColor(0xffffffff);
        avatarProgressView.setNoProgress(false);
        avatarContainer.addView(avatarProgressView, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.MATCH_PARENT));

        timeItem = new ImageView(context);
        timeItem.setPadding(AndroidUtilities.dp(10), AndroidUtilities.dp(10), AndroidUtilities.dp(5), AndroidUtilities.dp(5));
        timeItem.setScaleType(ImageView.ScaleType.CENTER);
        timeItem.setAlpha(0.0f);
        timeItem.setImageDrawable(timerDrawable = new TimerDrawable(context, null));
        timeItem.setTranslationY(-1);
        frameLayout.addView(timeItem, LayoutHelper.createFrame(34, 34, Gravity.TOP | Gravity.LEFT));

        starBgItem = new ImageView(context);
        starBgItem.setImageResource(R.drawable.star_small_outline);
        starBgItem.setColorFilter(new PorterDuffColorFilter(getThemedColor(Theme.key_actionBarDefault), PorterDuff.Mode.SRC_IN));
        starBgItem.setAlpha(0.0f);
        starBgItem.setScaleY(0.0f);
        starBgItem.setScaleX(0.0f);
        frameLayout.addView(starBgItem, LayoutHelper.createFrame(20, 20, Gravity.TOP | Gravity.LEFT));

        starFgItem = new ImageView(context);
        starFgItem.setImageResource(R.drawable.star_small_inner);
        starFgItem.setAlpha(0.0f);
        starFgItem.setScaleY(0.0f);
        starFgItem.setScaleX(0.0f);
        frameLayout.addView(starFgItem, LayoutHelper.createFrame(20, 20, Gravity.TOP | Gravity.LEFT));
        updateStar();

        showAvatarProgress(false, false);

        if (avatarsViewPager != null) {
            avatarsViewPager.onDestroy();
        }
        if (avatarsBlurView != null) {
            avatarsBlurView.destroy();
        }
//        if (metaball != null) {
//            metaball.destroy();
//        }
        overlaysView = new OverlaysView(context);
        avatarsBlurView = new ProfileGalleryBlurView(context);
        avatarsBlurView.setSize(getActionsExtraHeight());
        avatarsViewPager = new ProfileGalleryView(context, userId != 0 ? userId : -chatId, actionBar, listView, avatarImage, getClassGuid(), overlaysView, avatarsBlurView) {
            @Override
            protected void setCustomAvatarProgress(float progress) {
                customAvatarProgress = progress;
                checkPhotoDescriptionAlpha();
            }
        };
        if (userId != getUserConfig().clientUserId && userInfo != null) {
            customAvatarProgress = userInfo.profile_photo == null ? 0 : 1;
        }
        if (!isTopic) {
            avatarsViewPager.setChatInfo(chatInfo);
        }
        avatarContainer2.addView(avatarsViewPager);
        avatarContainer2.addView(avatarsBlurView, LayoutHelper.createFrame(-1, 74 + 64));
        avatarContainer2.addView(overlaysView);
        if (actionsView != null) {
            avatarsBlurView.setActionsView(actionsView);
            avatarContainer2.addView(actionsView, LayoutHelper.createFrame(-1, -1));
        }
        if (musicView != null) {
            avatarsBlurView.setMusicView(musicView);
            avatarContainer2.addView(musicView, LayoutHelper.createFrame(-1, -1));
        }
        avatarImage.setAvatarsViewPager(avatarsViewPager);

        avatarsViewPagerIndicatorView = new PagerIndicatorView(context);
        avatarContainer2.addView(avatarsViewPagerIndicatorView, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.MATCH_PARENT));

        frameLayout.addView(actionBar);

        float rightMargin = (54 + ((callItemVisible && userId != 0) ? 54 : 0));
        boolean hasTitleExpanded = false;
        int initialTitleWidth = LayoutHelper.WRAP_CONTENT;
        if (parentLayout != null && parentLayout.getLastFragment() instanceof ChatActivity) {
            ChatAvatarContainer avatarContainer = ((ChatActivity) parentLayout.getLastFragment()).getAvatarContainer();
            if (avatarContainer != null) {
                hasTitleExpanded = avatarContainer.getTitleTextView().getPaddingRight() != 0;
                if (avatarContainer.getLayoutParams() != null && avatarContainer.getTitleTextView() != null) {
                    rightMargin =
                            (((ViewGroup.MarginLayoutParams) avatarContainer.getLayoutParams()).rightMargin +
                                    (avatarContainer.getWidth() - avatarContainer.getTitleTextView().getRight())) / AndroidUtilities.density;
//                initialTitleWidth = (int) (avatarContainer.getTitleTextView().getWidth() / AndroidUtilities.density);
                }
            }
        }

        for (int a = 0; a < nameTextView.length; a++) {
            if (playProfileAnimation == 0 && a == 0) {
                continue;
            }
            nameTextView[a] = new SimpleTextView(context) {
                @Override
                public void onInitializeAccessibilityNodeInfo(AccessibilityNodeInfo info) {
                    super.onInitializeAccessibilityNodeInfo(info);
                    if (isFocusable() && (nameTextViewRightDrawableContentDescription != null || nameTextViewRightDrawable2ContentDescription != null)) {
                        StringBuilder s = new StringBuilder(getText());
                        if (nameTextViewRightDrawable2ContentDescription != null) {
                            if (s.length() > 0) s.append(", ");
                            s.append(nameTextViewRightDrawable2ContentDescription);
                        }
                        if (nameTextViewRightDrawableContentDescription != null) {
                            if (s.length() > 0) s.append(", ");
                            s.append(nameTextViewRightDrawableContentDescription);
                        }
                        info.setText(s);
                    }
                }

                @Override
                protected void onDraw(Canvas canvas) {
                    final int wasRightDrawableX = getRightDrawableX();
                    super.onDraw(canvas);
                    if (wasRightDrawableX != getRightDrawableX()) {
                        updateCollectibleHint();
                    }
                }

                @Override
                public void setTextSize(int sizeInDp) {
                    super.setTextSize(sizeInDp);
                }

                @Override
                public void setScaleX(float scaleX) {
                    super.setScaleX(scaleX);
                }
            };
            if (a == 1) {
                nameTextView[a].setTextColor(getThemedColor(Theme.key_profile_title));
            } else {
                nameTextView[a].setTextColor(getThemedColor(Theme.key_actionBarDefaultTitle));
            }
            nameTextView[a].setPadding(0, AndroidUtilities.dp(6), 0, AndroidUtilities.dp(a == 0 ? 12 : 4));
            nameTextView[a].setTextSize(18);
            nameTextView[a].setGravity(Gravity.LEFT);
            nameTextView[a].setTypeface(AndroidUtilities.bold());
            nameTextView[a].setLeftDrawableTopPadding(-AndroidUtilities.dp(1.3f));
            nameTextView[a].setPivotX(0);
            nameTextView[a].setPivotY(0);
            nameTextView[a].setAlpha(a == 0 ? 0.0f : 1.0f);
            if (a == 1) {
                nameTextView[a].setScrollNonFitText(true);
                nameTextView[a].setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);
            }
            nameTextView[a].setFocusable(a == 0);
            nameTextView[a].setEllipsizeByGradient(true);
            nameTextView[a].setRightDrawableOutside(a == 0);
            avatarContainer2.addView(nameTextView[a], LayoutHelper.createFrame(a == 0 ? initialTitleWidth : LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT, Gravity.LEFT | Gravity.TOP, 118, -6, (a == 0 ? rightMargin - (hasTitleExpanded ? 10 : 0) : 0), 0));
        }
        for (int a = 0; a < onlineTextView.length; a++) {
            if (a == 1) {
                onlineTextView[a] = new LinkSpanDrawable.ClickableSmallTextView(context) {

                    @Override
                    public void setAlpha(float alpha) {
                        super.setAlpha(alpha);
                        checkPhotoDescriptionAlpha();
                    }

                    @Override
                    public void setTranslationY(float translationY) {
                        super.setTranslationY(translationY);
                        lastRatingViewTranslationYOffset = getRatingViewTranslationYOffset();
                        onlineTextView[2].setTranslationY(translationY);
                        onlineTextView[3].setTranslationY(translationY);
                        if (ratingView != null) {
                            ratingView.setTranslationY(translationY - dp(5));
                        }
                    }

                    @Override
                    public void setTranslationX(float translationX) {
                        super.setTranslationX(translationX);
                        lastRatingViewTranslationXOffset = getRatingViewTranslationXOffset();
                        onlineTextView[2].setTranslationX(translationX);
                        onlineTextView[3].setTranslationX(translationX);
                        if (ratingView != null) {
                            ratingView.setTranslationX(translationX - getRatingViewTranslationXOffset());
                        }
                    }

                    @Override
                    public void setTextColor(int color) {
                        super.setTextColor(color);
                        if (onlineTextView[2] != null) {
                            onlineTextView[2].setTextColor(color);
                            onlineTextView[3].setTextColor(color);
                        }
                        if (showStatusButton != null) {
                            showStatusButton.setTextColor(Theme.multAlpha(Theme.adaptHSV(color, -.02f, +.15f), 1.4f));
                        }
                    }
                };
            } else {
                onlineTextView[a] = new LinkSpanDrawable.ClickableSmallTextView(context);
            }

            onlineTextView[a].setEllipsizeByGradient(true);
            onlineTextView[a].setTextColor(applyPeerColor(getThemedColor(Theme.key_actionBarDefaultSubtitle), true, null));
            onlineTextView[a].setTextSize(14);
            onlineTextView[a].setGravity(Gravity.LEFT);
            onlineTextView[a].setAlpha(a == 0 ? 0.0f : 1.0f);
            onlineTextView[a].setPivotX(dp(8));
            onlineTextView[a].setPivotY(dp(8));
            if (a == 1 || a == 2 || a == 3) {
                onlineTextView[a].setPadding(AndroidUtilities.dp(4), AndroidUtilities.dp(2), AndroidUtilities.dp(4), AndroidUtilities.dp(2));
            }
            if (a > 0) {
                onlineTextView[a].setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);
            }
            onlineTextView[a].setFocusable(a == 0);
            avatarContainer2.addView(onlineTextView[a], LayoutHelper.createFrame(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT, Gravity.LEFT | Gravity.TOP, 118 - (a == 1 || a == 2 || a == 3 ? 4 : 0), (a == 1 || a == 2 || a == 3 ? -2 : 0), (a == 0 ? rightMargin - (hasTitleExpanded ? 10 : 0) : 8) - (a == 1 || a == 2 || a == 3 ? 4 : 0), 0));
        }
        checkPhotoDescriptionAlpha();
        avatarContainer2.addView(animatedStatusView);

        ratingView = new StarRatingView(context);
        ratingView.setLayoutParams(LayoutHelper.createFrame(32, 32, Gravity.LEFT, 118 - 6, -2, 0, 0));
        ratingView.setResourcesProvider(resourcesProvider);
        checkStarRatingVisible();
        ratingView.setDelegate(visibility -> {
            onlineTextView[1].setTranslationX(getOnlineTextViewTranslationXWithOffsets(lastOnlineTextViewX));
            onlineTextView[1].setTranslationY(getOnlineTextViewTranslationYWithOffsets(lastOnlineTextViewY));
        });
        ratingView.setOnClickListener(this::showStarRatingBottomSheet);
        if (userInfo != null) {
            ratingView.set(userInfo.stars_rating);
        }

        avatarContainer2.addView(ratingView);

        mediaCounterTextView = new AudioPlayerAlert.ClippingTextViewSwitcher(context) {
            @Override
            protected TextView createTextView() {
                TextView textView = new TextView(context);
                textView.setTextColor(getThemedColor(Theme.key_player_actionBarSubtitle));
                textView.setTextSize(TypedValue.COMPLEX_UNIT_PX, AndroidUtilities.dp(14));
                textView.setSingleLine(true);
                textView.setEllipsize(TextUtils.TruncateAt.END);
                textView.setGravity(Gravity.LEFT);
                return textView;
            }
        };
        mediaCounterTextView.setAlpha(0.0f);
        avatarContainer2.addView(mediaCounterTextView, LayoutHelper.createFrame(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT, Gravity.LEFT | Gravity.TOP, 118.33f, -2, 8, 0));
        storyView = new ProfileStoriesView(context, currentAccount, getDialogId(), isTopic, avatarContainer, avatarImage, resourcesProvider) {
            @Override
            protected void onTap(StoryViewer.PlaceProvider provider) {
                long did = getDialogId();
                StoriesController storiesController = getMessagesController().getStoriesController();
                if (storiesController.hasStories(did) || storiesController.hasUploadingStories(did) || storiesController.isLastUploadingFailed(did)) {
                    getOrCreateStoryViewer().open(context, did, provider);
                } else if (userInfo != null && userInfo.stories != null && !userInfo.stories.stories.isEmpty() && userId != getUserConfig().clientUserId) {
                    getOrCreateStoryViewer().open(context, userInfo.stories, provider);
                } else if (chatInfo != null && chatInfo.stories != null && !chatInfo.stories.stories.isEmpty()) {
                    getOrCreateStoryViewer().open(context, chatInfo.stories, provider);
                } else {
                    expandAvatar();
                }
            }

            @Override
            protected void onLongPress() {
                openAvatar(false);
            }
        };
        updateStoriesViewBounds(false);
        if (userInfo != null) {
            storyView.setStories(userInfo.stories);
        } else if (chatInfo != null) {
            storyView.setStories(chatInfo.stories);
        }
        if (avatarImage != null) {
            avatarImage.setHasStories(needInsetForStories());
        }
        avatarContainer2.addView(storyView, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.MATCH_PARENT));
        giftsView = new ProfileGiftsView(context, currentAccount, getDialogId(), avatarContainer, avatarImage, resourcesProvider);
        avatarContainer2.addView(giftsView, 0, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.MATCH_PARENT));
        updateProfileData(true);

        writeButton = new RLottieImageView(context);

        if (actionsView != null) {
            writeButton.setVisibility(View.GONE);
        }
        writeButtonSetBackground();
        if (userId != 0) {
            if (imageUpdater != null) {
                cameraDrawable = new RLottieDrawable(R.raw.camera_outline, String.valueOf(R.raw.camera_outline), AndroidUtilities.dp(56), AndroidUtilities.dp(56), false, null);
                cellCameraDrawable = new RLottieDrawable(R.raw.camera_outline, R.raw.camera_outline + "_cell", AndroidUtilities.dp(42), AndroidUtilities.dp(42), false, null);

                if (actionsView != null) {
                    actionsView.beginApplyingActions();
                    actionsView.addCameraAction();
                    actionsView.addEditInfo();
                    actionsView.addSettings();
                    actionsView.commitActions();
                } else {
                    writeButton.setAnimation(cameraDrawable);
                    writeButton.setContentDescription(LocaleController.getString(R.string.AccDescrChangeProfilePicture));
                    writeButton.setPadding(AndroidUtilities.dp(2), 0, 0, AndroidUtilities.dp(2));
                }
            } else {
                if (actionsView != null) {
                    actionsView.set(ProfileActionsView.KEY_MESSAGE, true);
                }
                writeButton.setImageResource(R.drawable.profile_newmsg);
                writeButton.setContentDescription(LocaleController.getString(R.string.AccDescrOpenChat));
            }
        } else {
            writeButton.setImageResource(R.drawable.profile_discuss);
            writeButton.setContentDescription(LocaleController.getString(R.string.ViewDiscussion));
        }
        writeButton.setScaleType(ImageView.ScaleType.CENTER);

        frameLayout.addView(writeButton, LayoutHelper.createFrame(60, 60, Gravity.RIGHT | Gravity.TOP, 0, 0, 16, 0));
        writeButton.setOnClickListener(v -> {
            if (writeButton.getTag() != null) {
                return;
            }
            onWriteButtonClick();
        });
        needLayout(false);

        if (scrollTo != -1) {
            if (writeButtonTag != null) {
                writeButton.setTag(0);
                writeButton.setScaleX(0.2f);
                writeButton.setScaleY(0.2f);
                writeButton.setAlpha(0.0f);
            }
        }

        listView.setOnScrollListener(new RecyclerView.OnScrollListener() {

            @Override
            public void onScrollStateChanged(RecyclerView recyclerView, int newState) {
                if (newState == RecyclerView.SCROLL_STATE_DRAGGING) {
                    AndroidUtilities.hideKeyboard(getParentActivity().getCurrentFocus());
                }
                if (openingAvatar && newState != RecyclerView.SCROLL_STATE_SETTLING) {
                    openingAvatar = false;
                }
                if (searchItem != null) {
                    scrolling = newState != RecyclerView.SCROLL_STATE_IDLE;
                    searchItem.setEnabled(!scrolling && !isPulledDown);
                }
                sharedMediaLayout.scrollingByUser = listView.scrollingByUser;
            }

            @Override
            public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
                if (fwdRestrictedHint != null) {
                    fwdRestrictedHint.hide();
                }
                checkListViewScroll();
                if (participantsMap != null && !usersEndReached && layoutManager.findLastVisibleItemPosition() > membersEndRow - 8) {
                    getChannelParticipants(false);
                }
                sharedMediaLayout.setPinnedToTop(sharedMediaLayout.getY() <= 0);
                updateBottomButtonY();
            }
        });

        undoView = new UndoView(context, null, false, resourcesProvider);
        frameLayout.addView(undoView, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, Gravity.BOTTOM | Gravity.LEFT, 8, 0, 8, 8));

        expandAnimator = ValueAnimator.ofFloat(0f, 1f);
        expandAnimator.addUpdateListener(anim -> {
            setAvatarExpandProgress(anim.getAnimatedFraction());
        });
        expandAnimator.setInterpolator(CubicBezierInterpolator.EASE_OUT_QUINT);
        expandAnimator.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationStart(Animator animation) {

            }

            @Override
            public void onAnimationEnd(Animator animation) {
                actionBar.setItemsBackgroundColor(isPulledDown ? Theme.ACTION_BAR_WHITE_SELECTOR_COLOR : peerColor != null ? 0x20ffffff : getThemedColor(Theme.key_avatar_actionBarSelectorBlue), false);
                avatarImage.clearForeground();
                doNotSetForeground = false;
                updateStoriesViewBounds(false);
            }
        });
        updateRowsIds();

        updateSelectedMediaTabText();

        fwdRestrictedHint = new HintView(getParentActivity(), 9);
        fwdRestrictedHint.setAlpha(0);
        frameLayout.addView(fwdRestrictedHint, LayoutHelper.createFrame(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT, Gravity.LEFT | Gravity.TOP, 12, 0, 12, 0));
        sharedMediaLayout.setForwardRestrictedHint(fwdRestrictedHint);

        ViewGroup decorView;
        decorView = (ViewGroup) getParentActivity().getWindow().getDecorView();
        pinchToZoomHelper = new PinchToZoomHelper(decorView, frameLayout) {

            Paint statusBarPaint;

            @Override
            protected void invalidateViews() {
                super.invalidateViews();
                fragmentView.invalidate();
                for (int i = 0; i < avatarsViewPager.getChildCount(); i++) {
                    avatarsViewPager.getChildAt(i).invalidate();
                }
                if (writeButton != null) {
                    writeButton.invalidate();
                }
            }

            @Override
            protected void drawOverlays(Canvas canvas, float alpha, float parentOffsetX, float parentOffsetY, float clipTop, float clipBottom) {
                if (alpha > 0) {
                    AndroidUtilities.rectTmp.set(0, 0, avatarsViewPager.getMeasuredWidth(), avatarsViewPager.getMeasuredHeight() + AndroidUtilities.dp(30));
                    canvas.saveLayerAlpha(AndroidUtilities.rectTmp, (int) (255 * alpha), Canvas.ALL_SAVE_FLAG);

                    avatarContainer2.draw(canvas);

                    if (actionBar.getOccupyStatusBar() && !SharedConfig.noStatusBar) {
                        if (statusBarPaint == null) {
                            statusBarPaint = new Paint();
                            statusBarPaint.setColor(ColorUtils.setAlphaComponent(Color.BLACK, (int) (255 * 0.2f)));
                        }
                        canvas.drawRect(actionBar.getX(), actionBar.getY(), actionBar.getX() + actionBar.getMeasuredWidth(), actionBar.getY() + AndroidUtilities.statusBarHeight, statusBarPaint);
                    }
                    canvas.save();
                    canvas.translate(actionBar.getX(), actionBar.getY());
                    actionBar.draw(canvas);
                    canvas.restore();

                    if (writeButton != null && writeButton.getVisibility() == View.VISIBLE && writeButton.getAlpha() > 0) {
                        canvas.save();
                        float s = 0.5f + 0.5f * alpha;
                        canvas.scale(s, s, writeButton.getX() + writeButton.getMeasuredWidth() / 2f, writeButton.getY() + writeButton.getMeasuredHeight() / 2f);
                        canvas.translate(writeButton.getX(), writeButton.getY());
                        writeButton.draw(canvas);
                        canvas.restore();
                    }
                    canvas.restore();
                }
            }

            @Override
            protected boolean zoomEnabled(View child, ImageReceiver receiver) {
                if (!super.zoomEnabled(child, receiver)) {
                    return false;
                }
                return listView.getScrollState() != RecyclerView.SCROLL_STATE_DRAGGING;
            }
        };
        pinchToZoomHelper.setCallback(new PinchToZoomHelper.Callback() {
            @Override
            public void onZoomStarted(MessageObject messageObject) {
                listView.cancelClickRunnables(true);
                if (sharedMediaLayout != null && sharedMediaLayout.getCurrentListView() != null) {
                    sharedMediaLayout.getCurrentListView().cancelClickRunnables(true);
                }
                topView.setBackgroundColor(ColorUtils.blendARGB(getAverageColor(pinchToZoomHelper.getPhotoImage()), getThemedColor(Theme.key_windowBackgroundGray), 0.1f));
            }

            @Override
            public void onZoomFinished(MessageObject messageObject) {
                if (avatarsBlurView != null) {
                    avatarsBlurView.restartAlpha();
                }
            }
        });
        avatarsViewPager.setPinchToZoomHelper(pinchToZoomHelper);
        scrimPaint.setAlpha(0);
        actionBarBackgroundPaint.setColor(getThemedColor(Theme.key_listSelector));
        contentView.blurBehindViews.add(sharedMediaLayout);
        updateTtlIcon();

        blurredView = new View(context) {
            @Override
            public void setAlpha(float alpha) {
                super.setAlpha(alpha);
                if (fragmentView != null) {
                    fragmentView.invalidate();
                }
            }
        };
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            blurredView.setForeground(new ColorDrawable(ColorUtils.setAlphaComponent(getThemedColor(Theme.key_windowBackgroundWhite), 100)));
        }
        blurredView.setFocusable(false);
        blurredView.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);
        blurredView.setOnClickListener(e -> {
            finishPreviewFragment();
        });
        blurredView.setVisibility(View.GONE);
        blurredView.setFitsSystemWindows(true);
        contentView.addView(blurredView, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.MATCH_PARENT));

        createBirthdayEffect();
        if (myProfile) {
            contentView.addView(bottomButtonsContainer, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, 60, Gravity.BOTTOM | Gravity.FILL_HORIZONTAL));
        }

        if (actionsView != null && actionsView.hasCall()) {
            callToActionItem = new ImageView(context);
            callToActionItem.setScaleType(ImageView.ScaleType.CENTER);
            callToActionItem.setImageResource(R.drawable.call);
            callToActionItem.setVisibility(View.GONE);

            FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(
                    AndroidUtilities.dp(48),
                    ActionBar.getCurrentActionBarHeight()
            );
            lp.topMargin = actionBar.getOccupyStatusBar() ? AndroidUtilities.statusBarHeight : 0;
            lp.setMarginEnd(lp.width);
            lp.gravity = Gravity.END;
            frameLayout.addView(callToActionItem, lp);
        }

        bottomButton2Container = new FrameLayout(context);
        bottomButton2Container.setBackgroundColor(getThemedColor(Theme.key_windowBackgroundWhite));
        View buttonShadow = new View(context);
        buttonShadow.setBackgroundColor(getThemedColor(Theme.key_divider));
        bottomButton2Container.addView(buttonShadow, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, 1.0f / AndroidUtilities.density, Gravity.TOP | Gravity.FILL_HORIZONTAL, 0, 0, 0, 0));
        bottomButton2 = new ButtonWithCounterView(context, resourcesProvider);
        bottomButton2.setText(getString(R.string.Save), false);
        bottomButton2Container.addView(bottomButton2, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, 48, Gravity.FILL, 10, 10 + (1.0f / AndroidUtilities.density), 10, 10));
        bottomButton2.setOnClickListener(v -> {
            stopTabsReorder();
        });
        bottomButton2Container.setVisibility(View.GONE);
        bottomButton2Container.setTranslationY(dp(69));
        contentView.addView(bottomButton2Container, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, Gravity.BOTTOM | Gravity.FILL_HORIZONTAL));

        iBlur3Capture = (c, p) -> {
            listViewCapture.capture(c, p);
            if (sharedMediaLayout.iBlur3Capture != null) {
                sharedMediaLayout.iBlur3Capture.capture(c, p);
            }
        };

        ViewCompat.setOnApplyWindowInsetsListener(fragmentView, this::onApplyWindowInsets);
        return fragmentView;
    }

    private void toggleNoForwards(boolean enabled) {
        getMessagesController().toggleChatNoForwards(userId, 0, enabled, (res, err) -> {
            if (finishFragmentIfPreviousIsChatActivity()) {
                return;
            }

            if (BulletinFactory.canShowBulletin(ProfileActivity.this)) {
                if (res == MessagesController.TOGGLE_NO_FORWARDS_RESULT_OK) {
                    BulletinFactory.createDissableSharingBulletin(ProfileActivity.this, null, enabled).show();
                } else if (res == MessagesController.TOGGLE_NO_FORWARDS_RESULT_PENDING) {
                    BulletinFactory.createDissableSharingBulletin(ProfileActivity.this, DialogObject.getShortName(userId), enabled).show();
                } else if (err != null) {
                    BulletinFactory.showError(err);
                }
            }
            if (flagSecure != null) {
                flagSecure.invalidate();
            }
        });
    }

    public void startTabsReorder() {
        sharedMediaLayout.scrollSlidingTextTabStrip.setReordering(true);
        sharedMediaLayout.updateTabs(true);
        bottomButton2Container.setVisibility(View.VISIBLE);
        bottomButton2Container.animate()
            .translationY(0)
            .setDuration(180)
            .setInterpolator(CubicBezierInterpolator.EASE_OUT_QUINT)
            .start();
    }

    private void stopTabsReorder() {
        sharedMediaLayout.scrollSlidingTextTabStrip.setReordering(false);
        sharedMediaLayout.sendTabsOrder();
        sharedMediaLayout.updateTabs(true);
        bottomButton2Container.animate()
            .translationY(dp(69))
            .setDuration(180)
            .setInterpolator(CubicBezierInterpolator.EASE_OUT_QUINT)
            .withEndAction(() -> bottomButton2Container.setVisibility(View.GONE))
            .start();

        BulletinFactory.of(this)
            .createSimpleBulletin(R.raw.contact_check, "Tab order changed.")
            .show();
    }

    private void updateGooey() {
        float v = Math.min(pullUpProgress, 0.25f) / 0.25f;
        if (isTopic) {
            avatarGooey.setAlpha(1f - v);
            avatarGooey.setBlurIntensity(0f);
            avatarGooey.setGooeyEnabled(false);
        } else {
            avatarGooey.setPullProgress(pullUpProgress);
            avatarGooey.setBlurIntensity(Math.min((MathUtils.clamp(pullUpProgress, 0.2f, 0.7f) - 0.2f) / 0.5f, 0.75f));
            avatarGooey.setGooeyEnabled(pullUpProgress > 0 && pullUpProgress < 1);
        }
        if (storyView != null && playProfileAnimation != 2) {
            storyView.setAlpha(pullUpProgress > 0 ? lerp(1.0f, 0.0f, ilerp(pullUpProgress, 0, 0.5f)) : 1.0f);
        }
        avatarGooey.setVisibility(pullUpProgress >= 1.0f ? View.GONE : View.VISIBLE);
    }

    private int getHeaderOnlyExtraHeight() {
        if (getActionsExtraHeight() == 0) {
            return dp(168f);
        }
        return dp(152f);
    }

    private int getActionsExtraHeight() {
        return getActionsExtraHeight(true);
    }
    private int getActionsExtraHeight(boolean withMusic) {
        if (userId != 0 && imageUpdater != null && !myProfile)
            return 0;
        return dp(74 + (withMusic && hasMusic ? 25 : 0));
    }

    private int getHeaderExtraHeight() {
        return getHeaderOnlyExtraHeight() + getActionsExtraHeight();
    }

    private final BoolAnimator animatorBottomButtonVisibility = new BoolAnimator(0,
        (a, b, c, d) -> updateBottomButtonY(),
        CubicBezierInterpolator.EASE_OUT_QUINT, 380, true);

    private void updateBottomButtonY() {
        if (bottomButtonsContainer == null) {
            return;
        }

        float buttonsTranslationY;
        if (sharedMediaLayout != null && sharedMediaLayout.isAttachedToWindow()) {
            buttonsTranslationY = dp(72 + 160) - (listView.getMeasuredHeight() - sharedMediaLayout.getY());
        } else {
            buttonsTranslationY = dp(72);
        }
        animatorBottomButtonVisibility.setValue(buttonsTranslationY <= 0, true);

        final float factor = animatorBottomButtonVisibility.getFloatValue();
        bottomButtonsContainer.setTranslationY(lerp(dp(60), 0, factor));
        bottomButtonsContainer.setAlpha(factor);
        bottomButtonsContainer.setVisibility(factor > 0 ? View.VISIBLE : View.INVISIBLE);
        final Bulletin bulletin = Bulletin.getVisibleBulletin();
        if (bulletin != null) {
            bulletin.updatePosition();
        }
    }

    private void checkCanSendStoryForPosting() {
        TLRPC.Chat chat = MessagesController.getInstance(currentAccount).getChat(chatId);
        if (!ChatObject.isBoostSupported(chat)) {
            return;
        }
        StoriesController storiesController = getMessagesController().getStoriesController();
        storiesController.canSendStoryFor(getDialogId(), canSend -> {}, false, resourcesProvider);
    }

    private void updateAvatarRoundRadius() {
        avatarImage.setRoundRadiusForExpand((int) AndroidUtilities.lerp(getSmallAvatarRoundRadius(), 0f, currentExpandAnimatorValue));
    }

    private void onGiftPermiumClicked() {
        if (userInfo != null && UserObject.areGiftsDisabled(userInfo)) {
            BaseFragment lastFragment = LaunchActivity.getSafeLastFragment();
            if (lastFragment != null) {
                BulletinFactory.of(lastFragment).createSimpleBulletin(R.raw.error, AndroidUtilities.replaceTags(LocaleController.formatString(R.string.UserDisallowedGifts, DialogObject.getShortName(getDialogId())))).show();
            }
            return;
        }
        if (currentChat != null) {
            HintsController.Hint.ChannelGiftHint.doNotShowAgain();
        }
        showDialog(new GiftSheet(getContext(), currentAccount, getDialogId(), null, null));
    }

    private void onCallClicked(boolean isVideoCall) {
        if (userId != 0) {
            TLRPC.User user = getMessagesController().getUser(userId);
            if (user != null) {
                VoIPHelper.startCall(user, isVideoCall, userInfo != null && userInfo.video_calls_available, getParentActivity(), userInfo, getAccountInstance());
            }
        } else if (chatId != 0) {
            ChatObject.Call call = getMessagesController().getGroupCall(chatId, false);
            if (call == null) {
                VoIPHelper.showGroupCallAlert(ProfileActivity.this, currentChat, null, false, getAccountInstance());
            } else {
                VoIPHelper.startCall(currentChat, null, null, false, getParentActivity(), ProfileActivity.this, getAccountInstance());
            }
        }
    }

    private void onBlockContactClicked(boolean fromActions) {
        TLRPC.User user = getMessagesController().getUser(userId);
        if (user == null) {
            return;
        }
        if (!isBot || MessagesController.isSupportUser(user)) {
            if (userBlocked) {
                getMessagesController().unblockPeer(userId);
                if (BulletinFactory.canShowBulletin(ProfileActivity.this)) {
                    BulletinFactory.createBanBulletin(ProfileActivity.this, false).show();
                }
            } else {
                if (reportSpam) {
                    AlertsCreator.showBlockReportSpamAlert(ProfileActivity.this, userId, user, null, currentEncryptedChat, false, null, param -> {
                        if (param == 1) {
                            getNotificationCenter().removeObserver(ProfileActivity.this, NotificationCenter.closeChats);
                            getNotificationCenter().postNotificationName(NotificationCenter.closeChats);
                            playProfileAnimation = 0;
                            finishFragment();
                        } else {
                            getNotificationCenter().postNotificationName(NotificationCenter.peerSettingsDidLoad, userId);
                        }
                    }, resourcesProvider);
                } else {
                    AlertDialog.Builder builder = new AlertDialog.Builder(getParentActivity(), resourcesProvider);
                    builder.setTitle(LocaleController.getString(R.string.BlockUser));
                    builder.setMessage(AndroidUtilities.replaceTags(formatString("AreYouSureBlockContact2", R.string.AreYouSureBlockContact2, ContactsController.formatName(user.first_name, user.last_name))));
                    builder.setPositiveButton(LocaleController.getString(R.string.BlockContact), (dialogInterface, i) -> {
                        getMessagesController().blockPeer(userId);
                        if (BulletinFactory.canShowBulletin(ProfileActivity.this)) {
                            BulletinFactory.createBanBulletin(ProfileActivity.this, true).show();
                        }
                    });
                    builder.setNegativeButton(LocaleController.getString(R.string.Cancel), null);
                    AlertDialog dialog = builder.create();
                    showDialog(dialog);
                    TextView button = (TextView) dialog.getButton(DialogInterface.BUTTON_POSITIVE);
                    if (button != null) {
                        button.setTextColor(getThemedColor(Theme.key_text_RedBold));
                    }
                }
            }
        } else {
            if (!userBlocked || fromActions) {
                AlertsCreator.createClearOrDeleteDialogAlert(ProfileActivity.this, false, currentChat, user, currentEncryptedChat != null, true, false, true, (param) -> {
                    if (getParentLayout() != null) {
                        List<BaseFragment> fragmentStack = getParentLayout().getFragmentStack();
                        BaseFragment prevFragment = fragmentStack == null || fragmentStack.size() < 2 ? null : fragmentStack.get(fragmentStack.size() - 2);
                        if (prevFragment instanceof ChatActivity) {
                            getParentLayout().removeFragmentFromStack(fragmentStack.size() - 2);
                        }
                    }
                    disableProfileAnimation = true;
                    finishFragment();
                    getNotificationCenter().postNotificationName(NotificationCenter.needDeleteDialog, dialogId, user, currentChat, param);
                });
            } else {
                getMessagesController().unblockPeer(userId, () -> getSendMessagesHelper().sendMessage(SendMessagesHelper.SendMessageParams.of("/start", userId, null, null, null, false, null, null, null, true, 0, 0, null, false)));
                finishFragment();
            }
        }
    }

    private void onShareClicked() {
        try {
            String text = null;
            if (userId != 0) {
                TLRPC.User user = getMessagesController().getUser(userId);
                if (user == null) {
                    return;
                }
                if (botInfo != null && userInfo != null && !TextUtils.isEmpty(userInfo.about)) {
                    text = String.format("%s https://" + getMessagesController().linkPrefix + "/%s", userInfo.about, UserObject.getPublicUsername(user));
                } else {
                    text = String.format("https://" + getMessagesController().linkPrefix + "/%s", UserObject.getPublicUsername(user));
                }
            } else if (chatId != 0) {
                TLRPC.Chat chat = getMessagesController().getChat(chatId);
                if (chat == null) {
                    return;
                }
                if (chatInfo != null && !TextUtils.isEmpty(chatInfo.about)) {
                    text = String.format("%s\nhttps://" + getMessagesController().linkPrefix + "/%s", chatInfo.about, ChatObject.getPublicUsername(chat));
                } else {
                    text = String.format("https://" + getMessagesController().linkPrefix + "/%s", ChatObject.getPublicUsername(chat));
                }
            }
            if (TextUtils.isEmpty(text)) {
                return;
            }
            Intent intent = new Intent(Intent.ACTION_SEND);
            intent.setType("text/plain");
            intent.putExtra(Intent.EXTRA_TEXT, text);
            startActivityForResult(Intent.createChooser(intent, LocaleController.getString(R.string.BotShare)), 500);
        } catch (Exception e) {
            FileLog.e(e);
        }
    }

    private void collapseAvatarInstant() {
        if (allowPullingDown && currentExpandAnimatorValue > 0) {
            layoutManager.scrollToPositionWithOffset(0, getHeaderExtraHeight() - listView.getPaddingTop());
            listView.post(() -> {
                needLayout(true);
                if (expandAnimator.isRunning()) {
                    expandAnimator.cancel();
                }
                setAvatarExpandProgress(1f);
            });
        }
    }

    private boolean expandAvatar() {
        if ((hasMainTabs || !AndroidUtilities.isTablet() && !isInLandscapeMode) && avatarImage.getImageReceiver().hasNotThumb() && !AndroidUtilities.isAccessibilityScreenReaderEnabled()) {
            openingAvatar = true;
            allowPullingDown = true;
            View child = null;
            for (int i = 0; i < listView.getChildCount(); i++) {
                if (listView.getChildAdapterPosition(listView.getChildAt(i)) == 0) {
                    child = listView.getChildAt(i);
                    break;
                }
            }
            if (child != null) {
                RecyclerView.ViewHolder holder = listView.findContainingViewHolder(child);
                if (holder != null) {
                    Integer offset = positionToOffset.get(holder.getAdapterPosition());
                    if (offset != null) {
                        ignoreScrollOnFullExpand = true;
                        listView.smoothScrollBy(0, -(offset + (listView.getPaddingTop() - child.getTop() - actionBar.getMeasuredHeight())), CubicBezierInterpolator.EASE_OUT_QUINT);
                        return true;
                    }
                }
            }
        }
        return false;
    }

    private void setAvatarExpandProgress(float animatedFracture) {
        if (actionBar == null) {
            return;
        }

        final int newTop = ActionBar.getCurrentActionBarHeight() + (actionBar.getOccupyStatusBar() ? AndroidUtilities.statusBarHeight : 0);
        final float value = currentExpandAnimatorValue = AndroidUtilities.lerp(expandAnimatorValues, currentExpanAnimatorFracture = animatedFracture);
        checkPhotoDescriptionAlpha();

        avatarContainer.setScaleX(avatarScale);
        avatarContainer.setScaleY(avatarScale);
        avatarContainer.setTranslationY(lerp((float) Math.ceil(avatarY), 0f, value));
        avatarImage.setRoundRadiusForExpand((int) AndroidUtilities.lerp(getSmallAvatarRoundRadius(), 0f, value));
        avatarImage.setBlurRadiusProgressForExpand(value, avatarScale, isPulledDown);
        if (storyView != null) {
            storyView.setExpandProgress(value);
        }
        if (giftsView != null) {
            giftsView.setExpandProgress(value);
        }
        if (ratingView != null) {
            ratingView.setParentExpanded(value);
        }
        if (actionsView != null) {
            actionsView.setParentExpanded(value);
        }
        if (musicView != null) {
            musicView.setParentExpanded(value);
        }
        if (searchItem != null) {
            searchItem.setAlpha(1.0f - value);
            searchItem.setScaleY(1.0f - value);
            searchItem.setVisibility(View.VISIBLE);
            searchItem.setClickable(searchItem.getAlpha() > .5f);
        }

        final float diff = calculateHeaderExtraDiff();

        if (diff >= 1f) {
            avatarImage.setAlpha(1f);
        }

        if (!isPulledDown) {
            refreshNameAndOnlineYBasedOnExpand(diff, newTop);
        } else if (extraHeight >= getHeaderOnlyExtraHeight() && expandProgress < 0.33f) {
            refreshNameAndOnlineY();
        }

        if (scamDrawable != null) {
            scamDrawable.setColor(ColorUtils.blendARGB(getThemedColor(Theme.key_avatar_subtitleInProfileBlue), Color.argb(179, 255, 255, 255), value));
        }

        if (lockIconDrawable != null) {
            lockIconDrawable.setColorFilter(peerColor != null ? Color.WHITE : ColorUtils.blendARGB(getThemedColor(Theme.key_chat_lockIcon), Color.WHITE, value), PorterDuff.Mode.MULTIPLY);
        }

        if (verifiedCrossfadeDrawable[0] != null) {
            verifiedCrossfadeDrawable[0].setProgress(value);
        }
        if (verifiedCrossfadeDrawable[1] != null) {
            verifiedCrossfadeDrawable[1].setProgress(value);
        }

        if (premiumCrossfadeDrawable[0] != null) {
            premiumCrossfadeDrawable[0].setProgress(value);
        }
        if (premiumCrossfadeDrawable[1] != null) {
            premiumCrossfadeDrawable[1].setProgress(value);
        }

        updateEmojiStatusDrawableColor(value);

        float nameX = this.nameX;
        float onlineX = this.onlineX;

        if (diff < 1f) {
            nameX = AndroidUtilities.lerp(-dpf2(42 + 21), nameX, diff);
            onlineX = AndroidUtilities.lerp(-dpf2(42 + 21), onlineX, diff);
        }
        final float kx = dpf2(8);
        final float ky = isPulledDown ? dpf2(8) : dpf2(-24);

        final float nameTextViewXEnd = AndroidUtilities.dpf2(18f) - ((FrameLayout.LayoutParams) nameTextView[1].getLayoutParams()).leftMargin;
        final float nameTextViewYEnd = newTop + extraHeight - getActionsExtraHeight() - AndroidUtilities.dpf2(30f) - nameTextView[1].getBottom();
        final float nameTextViewCx = kx + nameX + (nameTextViewXEnd - nameX) / 2f;
        final float nameTextViewCy = ky + nameY + (nameTextViewYEnd - nameY) / 2f;
        final float nameTextViewX = (1 - value) * (1 - value) * nameX + 2 * (1 - value) * value * nameTextViewCx + value * value * nameTextViewXEnd;
        final float nameTextViewY = (1 - value) * (1 - value) * nameY + 2 * (1 - value) * value * nameTextViewCy + value * value * nameTextViewYEnd;

        final float onlineTextViewXEnd = AndroidUtilities.dpf2(16f) - ((FrameLayout.LayoutParams) onlineTextView[1].getLayoutParams()).leftMargin;
        final float onlineTextViewYEnd = newTop + extraHeight - getActionsExtraHeight() - AndroidUtilities.dpf2(10f) - onlineTextView[1].getBottom();
        final float onlineTextViewCx = kx + onlineX + (onlineTextViewXEnd - onlineX) / 2f;
        final float onlineTextViewCy = ky + onlineY + (onlineTextViewYEnd - onlineY) / 2f;
        final float onlineTextViewX = (1 - value) * (1 - value) * onlineX + 2 * (1 - value) * value * onlineTextViewCx + value * value * onlineTextViewXEnd;
        final float onlineTextViewY = (1 - value) * (1 - value) * onlineY + 2 * (1 - value) * value * onlineTextViewCy + value * value * onlineTextViewYEnd;

        final float minEndNameY = (actionBar.getOccupyStatusBar() ? AndroidUtilities.statusBarHeight : 0)
                + ActionBar.getCurrentActionBarHeight() / 2f
                - 21 * AndroidUtilities.density
                + actionBar.getTranslationY();

        final float minNameY = (float) Math.floor(minEndNameY) + AndroidUtilities.dp(1.3f);
        final float minOnlineY = minNameY + AndroidUtilities.dpf2(22.7f);

        nameTextView[1].setTranslationX(nameTextViewX);
        nameTextView[1].setTranslationY(Math.max(minNameY, nameTextViewY));
        onlineTextView[1].setTranslationX(getOnlineTextViewTranslationXWithOffsets(onlineTextViewX));
        onlineTextView[1].setTranslationY(getOnlineTextViewTranslationYWithOffsets(Math.max(minOnlineY, onlineTextViewY)));
        mediaCounterTextView.setTranslationX(onlineTextViewX);
        mediaCounterTextView.setTranslationY(Math.max(minOnlineY, onlineTextViewY));

        updateActionsPosition();
        updateMusicPosition();

        final Object onlineTextViewTag = onlineTextView[1].getTag();
        int statusColor;
        boolean online = false;
        if (onlineTextViewTag instanceof Integer) {
            statusColor = getThemedColor((Integer) onlineTextViewTag);
            online = (Integer) onlineTextViewTag == Theme.key_profile_status;
        } else {
            statusColor = getThemedColor(Theme.key_actionBarDefaultSubtitle);
        }
        onlineTextView[1].setTextColor(ColorUtils.blendARGB(applyPeerColor(statusColor, true, online), 0xB3FFFFFF, value));
        if (extraHeight > getHeaderOnlyExtraHeight()) {
            nameTextView[1].setPivotY(AndroidUtilities.lerp(0, nameTextView[1].getMeasuredHeight(), value));
            nameTextView[1].setScaleX(AndroidUtilities.lerp(1f + 0.12f * diff, 1.38f, value));
            nameTextView[1].setScaleY(AndroidUtilities.lerp(1f + 0.12f * diff, 1.38f, value));
        }
        needLayoutText(Math.min(1f, diff));

        if (showStatusButton != null) {
            showStatusButton.setBackgroundColor(ColorUtils.blendARGB(Theme.multAlpha(Theme.adaptHSV(actionBarBackgroundColor, +0.18f, -0.1f), 0.5f), 0x23ffffff, currentExpandAnimatorValue));
        }

        nameTextView[1].setTextColor(peerColor != null ? Color.WHITE : ColorUtils.blendARGB(getThemedColor(Theme.key_profile_title), Color.WHITE, currentExpandAnimatorValue));
        actionBar.setItemsColor(peerColor != null ? Color.WHITE : ColorUtils.blendARGB(getThemedColor(Theme.key_actionBarDefaultIcon), Color.WHITE, value), false);
        actionBar.setMenuOffsetSuppressed(true);

        avatarImage.setForegroundAlpha(value);

        final FrameLayout.LayoutParams params = (FrameLayout.LayoutParams) avatarContainer.getLayoutParams();
        params.width = (int) lerp(dpf2(100), listView.getMeasuredWidth() / avatarScale, value);
        params.height = (int) lerp(dpf2(100), (extraHeight + newTop) / avatarScale, value);
        params.leftMargin = (int) lerp(dpf2(64f), 0f, value);
        fixAvatarImageInCenter();
        avatarContainer.requestLayout();

        if (giftsView != null) {
            giftsView.setExpandProgress(value);
        }
        updateCollectibleHint();

        if (topView != null && topView.hasEmoji) {
            topView.invalidate();
        }
    }

    private float calculateHeaderExtraDiff() {
        return Utilities.clamp01((extraHeight - getActionsExtraHeight()) / (getHeaderOnlyExtraHeight()));
    }

    private void fixAvatarImageInCenter() {
        if (listView == null) return;
        final FrameLayout.LayoutParams params = (FrameLayout.LayoutParams) avatarContainer.getLayoutParams();
        avatarX = listView.getMeasuredWidth() / 2f - (params.leftMargin + params.width * avatarScale * 0.5f);
        if (openAnimationInProgress) {
            avatarX = lerp(0, avatarX, avatarAnimationProgress);
        }
        avatarContainer.setTranslationX(avatarX);
    }

    private int getSmallAvatarRoundRadius() {
        if (isTopic) {
            return 0;
        }
        if (chatId != 0) {
            TLRPC.Chat chatLocal = getMessagesController().getChat(chatId);
            if (ChatObject.isForum(chatLocal)) {
                return dp(needInsetForStories() ? 24 : 38);
            }
        }
        return dp(50);
    }

    private void updateTtlIcon() {
        if (ttlIconView == null) {
            return;
        }
        boolean visible = false;
        if (currentEncryptedChat == null) {
            if (userInfo != null && userInfo.ttl_period > 0) {
                visible = true;
            } else if (chatInfo != null && ChatObject.canUserDoAdminAction(currentChat, ChatObject.ACTION_DELETE_MESSAGES) && chatInfo.ttl_period > 0) {
                visible = true;
            }
        }
        AndroidUtilities.updateViewVisibilityAnimated(ttlIconView, visible, 0.8f, fragmentOpened);
    }

    public long getDialogId() {
        if (dialogId != 0) {
            return dialogId;
        } else if (userId != 0) {
            return userId;
        } else {
            return -chatId;
        }
    }

    public void getEmojiStatusLocation(Rect rect) {
        if (nameTextView[1] == null) {
            return;
        }
        if (nameTextView[1].getRightDrawable() == null) {
            rect.set(nameTextView[1].getWidth() - 1, nameTextView[1].getHeight() / 2 - 1, nameTextView[1].getWidth() + 1, nameTextView[1].getHeight() / 2 + 1);
            return;
        }
        rect.set(nameTextView[1].getRightDrawable().getBounds());
        rect.offset((int) (rect.centerX() * (nameTextView[1].getScaleX() - 1f)), 0);
        rect.offset((int) nameTextView[1].getX(), (int) nameTextView[1].getY());
    }

    private void onNotificationsClicked(boolean fromActions, float x, float y, View view) {
        final long did;
        if (dialogId != 0) {
            did = dialogId;
        } else if (userId != 0) {
            did = userId;
        } else {
            did = -chatId;
        }

        final boolean isCurrentMuted = getMessagesController().isDialogMuted(did, topicId);
        if (fromActions && (isTopic || isCurrentMuted)) {
            boolean newMuted = !isCurrentMuted;
            getNotificationsController().muteDialog(did, topicId, newMuted);
            BulletinFactory.createMuteBulletin(ProfileActivity.this, newMuted, null).show();
            updateExceptions();
            actionsView.setNotifications(!newMuted);
            return;
        }

        if (!fromActions && LocaleController.isRTL && x <= AndroidUtilities.dp(76) || !LocaleController.isRTL && x >= view.getMeasuredWidth() - AndroidUtilities.dp(76)) {
            NotificationsCheckCell checkCell = (NotificationsCheckCell) view;
            boolean checked = !checkCell.isChecked();

            boolean defaultEnabled = getNotificationsController().isGlobalNotificationsEnabled(did, false, false);

            String key = NotificationsController.getSharedPrefKey(did, topicId);
            if (checked) {
                SharedPreferences preferences = MessagesController.getNotificationsSettings(currentAccount);
                SharedPreferences.Editor editor = preferences.edit();
                if (defaultEnabled) {
                    editor.remove("notify2_" + key);
                } else {
                    editor.putInt("notify2_" + key, 0);
                }
                if (topicId == 0) {
                    getMessagesStorage().setDialogFlags(did, 0);
                    TLRPC.Dialog dialog = getMessagesController().dialogs_dict.get(did);
                    if (dialog != null) {
                        dialog.notify_settings = new TLRPC.TL_peerNotifySettings();
                    }
                }
                editor.apply();
            } else {
                int untilTime = Integer.MAX_VALUE;
                SharedPreferences preferences = MessagesController.getNotificationsSettings(currentAccount);
                SharedPreferences.Editor editor = preferences.edit();
                long flags;
                if (!defaultEnabled) {
                    editor.remove("notify2_" + key);
                    flags = 0;
                } else {
                    editor.putInt("notify2_" + key, 2);
                    flags = 1;
                }
                getNotificationsController().removeNotificationsForDialog(did);
                if (topicId == 0) {
                    getMessagesStorage().setDialogFlags(did, flags);
                    TLRPC.Dialog dialog = getMessagesController().dialogs_dict.get(did);
                    if (dialog != null) {
                        dialog.notify_settings = new TLRPC.TL_peerNotifySettings();
                        if (defaultEnabled) {
                            dialog.notify_settings.mute_until = untilTime;
                        }
                    }
                }
                editor.apply();
            }
            updateExceptions();
            getNotificationsController().updateServerNotificationsSettings(did, topicId);
            checkCell.setChecked(checked);
            RecyclerListView.Holder holder = (RecyclerListView.Holder) listView.findViewHolderForPosition(notificationsRow);
            updateNotifications(true);
            return;
        }
        ChatNotificationsPopupWrapper chatNotificationsPopupWrapper = new ChatNotificationsPopupWrapper(getContext(), currentAccount, null, true, true, new ChatNotificationsPopupWrapper.Callback() {
            @Override
            public void toggleSound() {
                SharedPreferences preferences = MessagesController.getNotificationsSettings(currentAccount);
                boolean enabled = !preferences.getBoolean("sound_enabled_" + NotificationsController.getSharedPrefKey(did, topicId), true);
                preferences.edit().putBoolean("sound_enabled_" + NotificationsController.getSharedPrefKey(did, topicId), enabled).apply();
                if (BulletinFactory.canShowBulletin(ProfileActivity.this)) {
                    BulletinFactory.createSoundEnabledBulletin(ProfileActivity.this, enabled ? NotificationsController.SETTING_SOUND_ON : NotificationsController.SETTING_SOUND_OFF, getResourceProvider()).show();
                }
            }

            @Override
            public void muteFor(int timeInSeconds) {
                if (timeInSeconds == 0) {
                    if (getMessagesController().isDialogMuted(did, topicId)) {
                        toggleMute();
                    }
                    if (BulletinFactory.canShowBulletin(ProfileActivity.this)) {
                        BulletinFactory.createMuteBulletin(ProfileActivity.this, NotificationsController.SETTING_MUTE_UNMUTE, timeInSeconds, getResourceProvider()).show();
                    }
                } else {
                    getNotificationsController().muteUntil(did, topicId, timeInSeconds);
                    if (BulletinFactory.canShowBulletin(ProfileActivity.this)) {
                        BulletinFactory.createMuteBulletin(ProfileActivity.this, NotificationsController.SETTING_MUTE_CUSTOM, timeInSeconds, getResourceProvider()).show();
                    }
                    updateExceptions();
                    updateNotifications(true);
                }
            }

            @Override
            public void showCustomize() {
                if (did != 0) {
                    Bundle args = new Bundle();
                    args.putLong("dialog_id", did);
                    args.putLong("topic_id", topicId);
                    presentFragment(new ProfileNotificationsActivity(args, resourcesProvider));
                }
            }

            @Override
            public void toggleMute() {
                boolean muted = getMessagesController().isDialogMuted(did, topicId);
                getNotificationsController().muteDialog(did, topicId, !muted);
                if (ProfileActivity.this.fragmentView != null) {
                    BulletinFactory.createMuteBulletin(ProfileActivity.this, !muted, null).show();
                }
                updateExceptions();
                updateNotifications(true);
            }

            @Override
            public void openExceptions() {
                Bundle bundle = new Bundle();
                bundle.putLong("dialog_id", did);
                TopicsNotifySettingsFragments notifySettings = new TopicsNotifySettingsFragments(bundle);
                notifySettings.setExceptions(notificationsExceptionTopics);
                presentFragment(notifySettings);
            }
        }, getResourceProvider());
        chatNotificationsPopupWrapper.update(did, topicId, notificationsExceptionTopics);
        if (AndroidUtilities.isTablet()) {
            final View v = parentLayout.getView();
            x += v.getX() + v.getPaddingLeft();
            y += v.getY() + v.getPaddingTop();
        }
        if (fromActions) {
            y += actionsView.getHeight() - dp(12);
        }
        chatNotificationsPopupWrapper.showAsOptions(ProfileActivity.this, view, x, y, fromActions);
    }

    private void updateNotifications(boolean updateList) {
        if (updateList && notificationsRow >= 0 && listAdapter != null) {
            listAdapter.notifyItemChanged(notificationsRow);
        }
        if (actionsView != null && !myProfile) {
            actionsView.setNotifications(isNotificationsEnabled());
        }
    }

    private boolean isNotificationsEnabled() {
        SharedPreferences preferences = MessagesController.getNotificationsSettings(currentAccount);
        long did;
        if (dialogId != 0) {
            did = dialogId;
        } else if (userId != 0) {
            did = userId;
        } else {
            did = -chatId;
        }
        String key = NotificationsController.getSharedPrefKey(did, topicId);
        boolean enabled = false;
        boolean hasOverride = preferences.contains("notify2_" + key);
        int value = preferences.getInt("notify2_" + key, 0);
        int delta = preferences.getInt("notifyuntil_" + key, 0);

        if (value == 3 && delta != Integer.MAX_VALUE) {
            delta -= getConnectionsManager().getCurrentTime();
            if (delta <= 0) {
                enabled = true;
            }
        } else {
            if (value == 0) {
                if (hasOverride) {
                    enabled = true;
                } else {
                    enabled = getNotificationsController().isGlobalNotificationsEnabled(did, false, false);
                }
            } else if (value == 1) {
                enabled = true;
            }
        }

        return enabled;
    }

    public void goToForum() {
        if (getParentLayout() != null && getParentLayout().getFragmentStack() != null) {
            for (int i = 0; i < getParentLayout().getFragmentStack().size(); ++i) {
                BaseFragment fragment = getParentLayout().getFragmentStack().get(i);
                if (fragment instanceof DialogsActivity) {
                    if (((DialogsActivity) fragment).rightSlidingDialogContainer != null) {
                        BaseFragment previewFragment = ((DialogsActivity) fragment).rightSlidingDialogContainer.getFragment();
                        if (previewFragment instanceof TopicsFragment && ((TopicsFragment) previewFragment).getDialogId() == getDialogId()) {
                            ((DialogsActivity) fragment).rightSlidingDialogContainer.finishPreview();
                        }
                    }
                } else if (fragment instanceof ChatActivity) {
                    if (((ChatActivity) fragment).getDialogId() == getDialogId()) {
                        getParentLayout().removeFragmentFromStack(fragment);
                        i--;
                    }
                } else if (fragment instanceof TopicsFragment) {
                    if (((TopicsFragment) fragment).getDialogId() == getDialogId()) {
                        getParentLayout().removeFragmentFromStack(fragment);
                        i--;
                    }
                } else if (fragment instanceof ProfileActivity) {
                    if (fragment != this && ((ProfileActivity) fragment).getDialogId() == getDialogId() && ((ProfileActivity) fragment).isTopic) {
                        getParentLayout().removeFragmentFromStack(fragment);
                        i--;
                    }
                }
            }
        }

        playProfileAnimation = 0;

        Bundle args = new Bundle();
        args.putLong("chat_id", chatId);
        presentFragment(TopicsFragment.getTopicsOrChat(this, args));
    }

    private SelectAnimatedEmojiDialog.SelectAnimatedEmojiDialogWindow selectAnimatedEmojiDialog;

    public void showStatusSelect() {
        if (selectAnimatedEmojiDialog != null) {
            return;
        }
        final SelectAnimatedEmojiDialog.SelectAnimatedEmojiDialogWindow[] popup = new SelectAnimatedEmojiDialog.SelectAnimatedEmojiDialogWindow[1];
        int xoff, yoff;
        getEmojiStatusLocation(AndroidUtilities.rectTmp2);
        int topMarginDp = nameTextView[1].getScaleX() < 1.5f ? 16 : 32;
        yoff = -(avatarContainer2.getHeight() - AndroidUtilities.rectTmp2.centerY()) - AndroidUtilities.dp(topMarginDp);
        int popupWidth = (int) Math.min(AndroidUtilities.dp(340 - 16), AndroidUtilities.displaySize.x * .95f);
        int ecenter = AndroidUtilities.rectTmp2.centerX();
        xoff = MathUtils.clamp(ecenter - popupWidth / 2, 0, AndroidUtilities.displaySize.x - popupWidth);
        ecenter -= xoff;
        SelectAnimatedEmojiDialog popupLayout = new SelectAnimatedEmojiDialog(this, getContext(), true, Math.max(0, ecenter), currentChat == null ? SelectAnimatedEmojiDialog.TYPE_EMOJI_STATUS : SelectAnimatedEmojiDialog.TYPE_EMOJI_STATUS_CHANNEL, true, resourcesProvider, topMarginDp) {
            @Override
            protected boolean willApplyEmoji(View view, Long documentId, TLRPC.Document document, TL_stars.TL_starGiftUnique gift, Integer until) {
                if (gift != null) {
                    final TL_stars.SavedStarGift savedStarGift = StarsController.getInstance(currentAccount).findUserStarGift(gift.id);
                    return savedStarGift == null || MessagesController.getGlobalMainSettings().getInt("statusgiftpage", 0) >= 2;
                }
                return true;
            }

            @Override
            public long getDialogId() {
                return ProfileActivity.this.getDialogId();
            }

            @Override
            protected void onEmojiSelected(View emojiView, Long documentId, TLRPC.Document document, TL_stars.TL_starGiftUnique gift, Integer until) {
                final TLRPC.EmojiStatus emojiStatus;
                if (gift != null) {
                    final TL_stars.SavedStarGift savedStarGift = StarsController.getInstance(currentAccount).findUserStarGift(gift.id);
                    if (savedStarGift != null && MessagesController.getGlobalMainSettings().getInt("statusgiftpage", 0) < 2) {
                        MessagesController.getGlobalMainSettings().edit().putInt("statusgiftpage", MessagesController.getGlobalMainSettings().getInt("statusgiftpage", 0) + 1).apply();
                        new StarGiftSheet(getContext(), currentAccount, UserConfig.getInstance(currentAccount).getClientUserId(), resourcesProvider)
                                .set(savedStarGift, null)
                                .setupWearPage()
                                .show();
                        if (popup[0] != null) {
                            selectAnimatedEmojiDialog = null;
                            popup[0].dismiss();
                        }
                        return;
                    }
                    final TLRPC.TL_inputEmojiStatusCollectible status = new TLRPC.TL_inputEmojiStatusCollectible();
                    status.collectible_id = gift.id;
                    if (until != null) {
                        status.flags |= 1;
                        status.until = until;
                    }
                    emojiStatus = status;
                } else if (documentId == null) {
                    emojiStatus = new TLRPC.TL_emojiStatusEmpty();
                } else {
                    final TLRPC.TL_emojiStatus status = new TLRPC.TL_emojiStatus();
                    status.document_id = documentId;
                    if (until != null) {
                        status.flags |= 1;
                        status.until = until;
                    }
                    emojiStatus = status;
                }
                emojiStatusGiftId = gift != null ? gift.id : null;
                getMessagesController().updateEmojiStatus(currentChat == null ? 0 : -currentChat.id, emojiStatus, gift);
                for (int a = 0; a < 2; ++a) {
                    if (emojiStatusDrawable[a] != null) {
                        if (documentId == null && currentChat == null) {
                            emojiStatusDrawable[a].set(getPremiumCrossfadeDrawable(a), true);
                        } else if (documentId != null) {
                            emojiStatusDrawable[a].set(documentId, true);
                        } else {
                            emojiStatusDrawable[a].set((Drawable) null, true);
                        }
                        emojiStatusDrawable[a].setParticles(gift != null, true);
                    }
                }
                if (documentId != null) {
                    animatedStatusView.animateChange(ReactionsLayoutInBubble.VisibleReaction.fromCustomEmoji(documentId));
                }
                updateEmojiStatusDrawableColor();
                updateEmojiStatusEffectPosition();
                if (popup[0] != null) {
                    selectAnimatedEmojiDialog = null;
                    popup[0].dismiss();
                }
            }
        };
        TLRPC.User user = getMessagesController().getUser(userId);
        if (user != null) {
            popupLayout.setExpireDateHint(DialogObject.getEmojiStatusUntil(user.emoji_status));
        }
        if (emojiStatusGiftId != null) {
            popupLayout.setSelected(emojiStatusGiftId);
        } else {
            popupLayout.setSelected(emojiStatusDrawable[1] != null && emojiStatusDrawable[1].getDrawable() instanceof AnimatedEmojiDrawable ? ((AnimatedEmojiDrawable) emojiStatusDrawable[1].getDrawable()).getDocumentId() : null);
        }
        popupLayout.setSaveState(3);
        popupLayout.setScrimDrawable(emojiStatusDrawable[1], nameTextView[1]);
        popup[0] = selectAnimatedEmojiDialog = new SelectAnimatedEmojiDialog.SelectAnimatedEmojiDialogWindow(popupLayout, LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT) {
            @Override
            public void dismiss() {
                super.dismiss();
                selectAnimatedEmojiDialog = null;
            }
        };
        int[] loc = new int[2];
        if (nameTextView[1] != null) {
            nameTextView[1].getLocationOnScreen(loc);
        }
        popup[0].showAsDropDown(fragmentView, xoff, yoff, Gravity.TOP | Gravity.LEFT);
        popup[0].dimBehind();
    }

    public TLRPC.Chat getCurrentChat() {
        return currentChat;
    }

    public TLRPC.ChatFull getChatInfo() {
        return chatInfo;
    }

    public TLRPC.UserFull getUserInfo() {
        return userInfo;
    }

    @Override
    public boolean isFragmentOpened() {
        return isFragmentOpened;
    }

    private void openAvatar() {
        openAvatar(false);
    }
    private void openAvatar(boolean ignoreDragging) {
        if (listView.getScrollState() == RecyclerView.SCROLL_STATE_DRAGGING && !ignoreDragging) {
            return;
        }
        int carouselPosition = avatarsViewPager != null ? avatarsViewPager.getRealPosition() : 0;
        ImageLocation carouselImageLocation = avatarsViewPager != null ? avatarsViewPager.getRealImageLocation(carouselPosition) : null;
        TLRPC.Photo carouselPhoto = avatarsViewPager != null ? avatarsViewPager.getPhoto(carouselPosition) : null;
        if (userId != 0) {
            TLRPC.User user = getMessagesController().getUser(userId);
            if (user.photo != null && user.photo.photo_big != null) {
                PhotoViewer.getInstance().setParentActivity(ProfileActivity.this);
                if (user.photo.dc_id != 0) {
                    user.photo.photo_big.dc_id = user.photo.dc_id;
                }
                TLRPC.FileLocation fileLocation = carouselImageLocation != null && carouselImageLocation.location != null ? carouselImageLocation.location : user.photo.photo_big;
                TLRPC.Photo photo = carouselPhoto;
                if (photo == null && userInfo != null) {
                    if (user.photo.personal && userInfo.personal_photo instanceof TLRPC.TL_photo) {
                        photo = userInfo.personal_photo;
                    } else if (userInfo.profile_photo instanceof TLRPC.TL_photo && userInfo.profile_photo.id == user.photo.photo_id) {
                        photo = userInfo.profile_photo;
                    } else if (userInfo.fallback_photo instanceof TLRPC.TL_photo && userInfo.fallback_photo.id == user.photo.photo_id) {
                        photo = userInfo.fallback_photo;
                    } else if (userInfo.profile_photo instanceof TLRPC.TL_photo) {
                        photo = userInfo.profile_photo;
                    }
                }
                ImageLocation videoLocation = null;
                if (photo != null && !photo.video_sizes.isEmpty()) {
                    TLRPC.VideoSize videoSize = FileLoader.getClosestVideoSizeWithSize(photo.video_sizes, 1000);
                    if (videoSize != null) {
                        videoLocation = ImageLocation.getForPhoto(videoSize, photo);
                    }
                }
                PhotoViewer.getInstance().openPhotoWithVideo(fileLocation, carouselImageLocation, videoLocation, provider);
            }
        } else if (chatId != 0) {
            TLRPC.Chat chat = getMessagesController().getChat(chatId);
            if (chat.photo != null && chat.photo.photo_big != null) {
                PhotoViewer.getInstance().setParentActivity(ProfileActivity.this);
                if (chat.photo.dc_id != 0) {
                    chat.photo.photo_big.dc_id = chat.photo.dc_id;
                }
                TLRPC.FileLocation fileLocation = carouselImageLocation != null && carouselImageLocation.location != null ? carouselImageLocation.location : chat.photo.photo_big;
                TLRPC.Photo photo = carouselPhoto;
                if (photo == null && chatInfo != null && chatInfo.chat_photo instanceof TLRPC.TL_photo) {
                    photo = chatInfo.chat_photo;
                }
                ImageLocation videoLocation = null;
                if (photo != null && !photo.video_sizes.isEmpty()) {
                    TLRPC.VideoSize videoSize = FileLoader.getClosestVideoSizeWithSize(photo.video_sizes, 1000);
                    if (videoSize != null) {
                        videoLocation = ImageLocation.getForPhoto(videoSize, photo);
                    }
                }
                PhotoViewer.getInstance().openPhotoWithVideo(fileLocation, carouselImageLocation, videoLocation, provider);
            }
        }
    }

    private void onJoinClicked(boolean fromActions) {
        BaseFragment lastFragment = parentLayout.getLastFragment();
        final boolean[] result = new boolean[]{true};
        getMessagesController().addUserToChat(currentChat.id, getUserConfig().getCurrentUser(), 0, null, ProfileActivity.this, true, () -> {
            if (!fromActions || joinRow != -1) {
                updateRowsIds();
                if (listAdapter != null) {
                    listAdapter.notifyDataSetChanged();
                }
            } else if (actionsView != null) {
                actionsView.stopLoading(ProfileActionsView.KEY_JOIN);
                if (result[0]) {
                    actionsView.beginApplyingActions();
                    actionsView.set(ProfileActionsView.KEY_JOIN, false);
                    actionsView.set(ProfileActionsView.KEY_LEAVE, true);
                    actionsView.commitActions();
                }
            }
        }, err -> {
            result[0] = false;
            if (err != null && "INVITE_REQUEST_SENT".equals(err.text)) {
                SharedPreferences preferences = MessagesController.getNotificationsSettings(currentAccount);
                preferences.edit().putLong("dialog_join_requested_time_" + dialogId, System.currentTimeMillis()).commit();
                JoinGroupAlert.showBulletin(getContext(), ProfileActivity.this, ChatObject.isChannel(currentChat) && !currentChat.megagroup);
                if (!fromActions || joinRow != -1) {
                    updateRowsIds();
                    if (listAdapter != null) {
                        listAdapter.notifyDataSetChanged();
                    }
                }
                if (lastFragment instanceof ChatActivity) {
                    ((ChatActivity) lastFragment).showBottomOverlayProgress(false, true);
                }
                return false;
            }
            return true;
        });
        NotificationCenter.getGlobalInstance().postNotificationName(NotificationCenter.closeSearchByActiveAction);
    }

    private void onWriteButtonClick() {
        if (userId != 0) {
            if (imageUpdater != null) {
                TLRPC.User user = MessagesController.getInstance(currentAccount).getUser(UserConfig.getInstance(currentAccount).getClientUserId());
                if (user == null) {
                    user = UserConfig.getInstance(currentAccount).getCurrentUser();
                }
                if (user == null) {
                    return;
                }
                imageUpdater.openMenu(user.photo != null && user.photo.photo_big != null && !(user.photo instanceof TLRPC.TL_userProfilePhotoEmpty), () -> {
                    MessagesController.getInstance(currentAccount).deleteUserPhoto(null);
                    cameraDrawable.setCurrentFrame(0);
                    cellCameraDrawable.setCurrentFrame(0);
                }, dialog -> {
                    if (!imageUpdater.isUploadingImage()) {
                        cameraDrawable.setCustomEndFrame(86);
                        cellCameraDrawable.setCustomEndFrame(86);
                        if (actionsView != null) {
                            actionsView.startCameraAnimation();
                        } else {
                            writeButton.playAnimation();
                        }
                        if (setAvatarCell != null) {
                            setAvatarCell.getImageView().playAnimation();
                        }
                    } else {
                        cameraDrawable.setCurrentFrame(0, false);
                        cellCameraDrawable.setCurrentFrame(0, false);
                    }
                }, 0);
                cameraDrawable.setCurrentFrame(0);
                cameraDrawable.setCustomEndFrame(43);
                cellCameraDrawable.setCurrentFrame(0);
                cellCameraDrawable.setCustomEndFrame(43);
                if (actionsView != null) {
                    actionsView.startCameraAnimation();
                } else {
                    writeButton.playAnimation();
                }
                if (setAvatarCell != null) {
                    setAvatarCell.getImageView().playAnimation();
                }
            } else {
                openChat();
            }
        } else {
            openDiscussion();
        }
    }

    private void openChat() {
        if (userId != 0) {
            if (finishFragmentIfPreviousIsChatActivity()) {
                return;
            }

            TLRPC.User user = getMessagesController().getUser(userId);
            if (user == null || user instanceof TLRPC.TL_userEmpty) {
                return;
            }
            Bundle args = new Bundle();
            args.putLong("user_id", userId);
            if (!getMessagesController().checkCanOpenChat(args, ProfileActivity.this)) {
                return;
            }
            boolean removeFragment = arguments.getBoolean("removeFragmentOnChatOpen", true);
            if (!AndroidUtilities.isTablet() && removeFragment) {
                getNotificationCenter().removeObserver(ProfileActivity.this, NotificationCenter.closeChats);
                getNotificationCenter().postNotificationName(NotificationCenter.closeChats);
            }
            int distance = getArguments().getInt("nearby_distance", -1);
            if (distance >= 0) {
                args.putInt("nearby_distance", distance);
            }
            ChatActivity chatActivity = new ChatActivity(args);
            chatActivity.setPreloadedSticker(getMediaDataController().getGreetingsSticker(), false);
            presentFragment(chatActivity, removeFragment);
            if (AndroidUtilities.isTablet() && !hasMainTabs) {
                finishFragment();
            }
        }
    }

    private boolean finishFragmentIfPreviousIsChatActivity() {
        if (playProfileAnimation != 0 && parentLayout != null && parentLayout.getFragmentStack() != null && parentLayout.getFragmentStack().size() >= 2 && parentLayout.getFragmentStack().get(parentLayout.getFragmentStack().size() - 2) instanceof ChatActivity) {
            finishFragment();
            return true;
        }
        return false;
    }

    private void openTopic() {
        if (isTopic) {
            finishFragmentIfPreviousIsChatActivity();
        }
    }

    private void openGroup() {
        if (finishFragmentIfPreviousIsChatActivity()) {
            return;
        }

        Bundle args = new Bundle();
        args.putLong("chat_id", chatId);
        if (!getMessagesController().checkCanOpenChat(args, ProfileActivity.this)) {
            return;
        }
        ChatActivity chatActivity = new ChatActivity(args);
        presentFragment(chatActivity, false);
    }

    private void openForum() {
        Bundle args = new Bundle();
        args.putLong("chat_id", chatId);
        if (!getMessagesController().checkCanOpenChat(args, ProfileActivity.this)) {
            return;
        }
        ChatActivity chatActivity = new ChatActivity(args);
        presentFragment(chatActivity, false);
    }

    private void openDiscussion() {
        if (chatInfo == null || chatInfo.linked_chat_id == 0) {
            return;
        }
        Bundle args = new Bundle();
        args.putLong("chat_id", chatInfo.linked_chat_id);
        if (!getMessagesController().checkCanOpenChat(args, ProfileActivity.this)) {
            return;
        }
        presentFragment(new ChatActivity(args));
    }

    public boolean onMemberClick(TLRPC.ChatParticipant participant, boolean isLong, View view) {
        return onMemberClick(participant, isLong, false, view);
    }

    public boolean onMemberClick(TLRPC.ChatParticipant participant, boolean isLong, boolean resultOnly) {
        return onMemberClick(participant, isLong, resultOnly, null);
    }

    public boolean onMemberClick(TLRPC.ChatParticipant participant, boolean isLong, boolean resultOnly, View view) {
        if (getParentActivity() == null) {
            return false;
        }
        if (isLong) {
            TLRPC.User user = getMessagesController().getUser(participant.user_id);
            if (user == null) {// || participant.user_id == getUserConfig().getClientUserId()) {
                return false;
            }
            selectedUser = participant.user_id;
            final boolean self = participant.user_id == getUserConfig().getClientUserId();
            boolean allowKick;
            boolean canEditAdmin;
            boolean canRestrict;
            boolean editingAdmin;
            boolean canEditTag;
            final String rank;
            final boolean isAdmin, isOwner;
            final TLRPC.ChannelParticipant channelParticipant;

            if (ChatObject.isChannel(currentChat)) {
                channelParticipant = ((TLRPC.TL_chatChannelParticipant) participant).channelParticipant;
                rank = channelParticipant.rank;
                isAdmin = channelParticipant instanceof TLRPC.TL_channelParticipantCreator || channelParticipant instanceof TLRPC.TL_channelParticipantAdmin;
                isOwner = channelParticipant instanceof TLRPC.TL_channelParticipantCreator;
                final TLRPC.User u = getMessagesController().getUser(participant.user_id);
                canEditAdmin = ChatObject.canAddAdmins(currentChat);
                canEditTag = ChatObject.canManageTags(currentChat) && (!isAdmin || !isOwner && channelParticipant.can_edit || self);
                if (channelParticipant instanceof TLRPC.TL_channelParticipantCreator || channelParticipant instanceof TLRPC.TL_channelParticipantAdmin && !channelParticipant.can_edit) {
                    canEditAdmin = false;
                    canEditTag = false;
                }
                if ((self || channelParticipant instanceof TLRPC.TL_channelParticipantSelf) && ChatObject.canManageMyTag(currentChat)) {
                    canEditTag = true;
                }
                allowKick = canRestrict = ChatObject.canBlockUsers(currentChat) && (!(channelParticipant instanceof TLRPC.TL_channelParticipantAdmin || channelParticipant instanceof TLRPC.TL_channelParticipantCreator) || channelParticipant.can_edit);
                if (currentChat.gigagroup) {
                    canRestrict = false;
                }
                editingAdmin = channelParticipant instanceof TLRPC.TL_channelParticipantAdmin;
            } else {
                channelParticipant = null;
                rank = participant.rank;
                isAdmin = participant instanceof TLRPC.TL_chatParticipantAdmin || participant instanceof TLRPC.TL_chatParticipantCreator;
                isOwner = participant instanceof TLRPC.TL_chatParticipantCreator;
                allowKick = currentChat.creator || participant instanceof TLRPC.TL_chatParticipant && (ChatObject.canBlockUsers(currentChat) || participant.inviter_id == getUserConfig().getClientUserId());
                canEditAdmin = currentChat.creator;
                canEditTag = ChatObject.canManageTags(currentChat) && (!isAdmin || !isOwner && participant.inviter_id == getUserConfig().getClientUserId() || self) || self && ChatObject.canManageMyTag(currentChat);
                canRestrict = currentChat.creator;
                editingAdmin = participant instanceof TLRPC.TL_chatParticipantAdmin;
            }
            if (self) {
                canEditAdmin = false;
                allowKick = false;
                canRestrict = false;
                if (ChatObject.canManageMyTag(currentChat) || isAdmin && ChatObject.canManageTags(currentChat)) {
                    canEditTag = true;
                }
            }

            boolean result = (canEditAdmin || canEditTag || canRestrict || allowKick);
            if (resultOnly || !result) {
                return result;
            }

            Utilities.Callback<Integer> openRightsEdit = action -> {
                if (channelParticipant != null) {
                    openRightsEdit(action, user, participant, channelParticipant.admin_rights, channelParticipant.banned_rights, channelParticipant.rank, editingAdmin);
                } else {
                    openRightsEdit(action, user, participant, null, null, rank, editingAdmin);
                }
            };

            Drawable bg = null;
            if (view.getParent() instanceof RecyclerListView) {
                bg = ((RecyclerListView) view.getParent()).getClipBackground(view);
            }

            ItemOptions.makeOptions(this, view)
                    .setScrimViewBackground(bg)
                    .addIf(!self, R.drawable.msg_discussion, getString(R.string.SendMessage), () -> {
                        presentFragment(ChatActivity.of(user.id));
                    })
                    .addGapIf(!self && (canEditAdmin || canEditTag || canRestrict || allowKick))
                    .addIf(canEditTag, !isAdmin && TextUtils.isEmpty(rank) ? R.drawable.menu_tag_plus : R.drawable.menu_tag_edit, getString(isAdmin ? R.string.EditAdminTag : TextUtils.isEmpty(rank) ? R.string.AddMemberTag : R.string.EditMemberTag), () -> {
                        TagEditCell.showSheet(getContext(), currentAccount, getDialogId(), user, rank, isAdmin, isOwner, resourcesProvider);
                    })
                    .addIf(canEditAdmin, R.drawable.msg_admins, editingAdmin ? LocaleController.getString(R.string.EditAdminRights) : LocaleController.getString(R.string.SetAsAdmin), () -> openRightsEdit.run(0))
                    .addIf(canRestrict, R.drawable.msg_permissions, LocaleController.getString(R.string.ChangePermissions), () -> {
                        if (channelParticipant instanceof TLRPC.TL_channelParticipantAdmin || participant instanceof TLRPC.TL_chatParticipantAdmin) {
                            showDialog(
                                    new AlertDialog.Builder(getParentActivity(), resourcesProvider)
                                            .setTitle(LocaleController.getString(R.string.AppName))
                                            .setMessage(formatString(R.string.AdminWillBeRemoved, ContactsController.formatName(user.first_name, user.last_name)))
                                            .setPositiveButton(LocaleController.getString(R.string.OK), (dialog, which) -> openRightsEdit.run(1))
                                            .setNegativeButton(LocaleController.getString(R.string.Cancel), null)
                                            .create()
                            );
                        } else {
                            openRightsEdit.run(1);
                        }
                    })
                    .addIf(allowKick, R.drawable.msg_remove, LocaleController.getString(R.string.KickFromGroup), true, () -> {
                        kickUser(selectedUser, participant);
                    })
                    .setMinWidth(190)
                    .show();
        } else {
            if (participant.user_id == getUserConfig().getClientUserId()) {
                return false;
            }
            Bundle args = new Bundle();
            args.putLong("user_id", participant.user_id);
            args.putBoolean("preload_messages", true);
            presentFragment(new ProfileActivity(args));
        }
        return true;
    }

    private void openRightsEdit(int action, TLRPC.User user, TLRPC.ChatParticipant participant, TLRPC.TL_chatAdminRights adminRights, TLRPC.TL_chatBannedRights bannedRights, String rank, boolean editingAdmin) {
        boolean[] needShowBulletin = new boolean[1];
        ChatRightsEditActivity fragment = new ChatRightsEditActivity(user.id, chatId, adminRights, currentChat.default_banned_rights, bannedRights, rank, action, true, false, null) {
            @Override
            public void onTransitionAnimationEnd(boolean isOpen, boolean backward) {
                if (!isOpen && backward && needShowBulletin[0] && BulletinFactory.canShowBulletin(ProfileActivity.this)) {
                    BulletinFactory.createPromoteToAdminBulletin(ProfileActivity.this, user.first_name).show();
                }
            }
        };
        fragment.setDelegate(new ChatRightsEditActivity.ChatRightsEditActivityDelegate() {
            @Override
            public void didSetRights(int rights, TLRPC.TL_chatAdminRights rightsAdmin, TLRPC.TL_chatBannedRights rightsBanned, String rank) {
                if (action == 0) {
                    if (participant instanceof TLRPC.TL_chatChannelParticipant) {
                        TLRPC.TL_chatChannelParticipant channelParticipant1 = ((TLRPC.TL_chatChannelParticipant) participant);
                        if (rights == 1) {
                            channelParticipant1.channelParticipant = new TLRPC.TL_channelParticipantAdmin();
                            channelParticipant1.channelParticipant.flags |= 4;
                        } else {
                            channelParticipant1.channelParticipant = new TLRPC.TL_channelParticipant();
                        }
                        channelParticipant1.channelParticipant.inviter_id = getUserConfig().getClientUserId();
                        channelParticipant1.channelParticipant.peer = new TLRPC.TL_peerUser();
                        channelParticipant1.channelParticipant.peer.user_id = participant.user_id;
                        channelParticipant1.channelParticipant.date = participant.date;
                        channelParticipant1.channelParticipant.banned_rights = rightsBanned;
                        channelParticipant1.channelParticipant.admin_rights = rightsAdmin;
                        channelParticipant1.channelParticipant.rank = rank;
                    } else if (participant != null) {
                        TLRPC.ChatParticipant newParticipant;
                        if (rights == 1) {
                            newParticipant = new TLRPC.TL_chatParticipantAdmin();
                        } else {
                            newParticipant = new TLRPC.TL_chatParticipant();
                        }
                        newParticipant.user_id = participant.user_id;
                        newParticipant.date = participant.date;
                        newParticipant.inviter_id = participant.inviter_id;
                        int index = chatInfo.participants.participants.indexOf(participant);
                        if (index >= 0) {
                            chatInfo.participants.participants.set(index, newParticipant);
                        }
                    }
                    if (rights == 1 && !editingAdmin) {
                        needShowBulletin[0] = true;
                    }
                } else if (action == 1) {
                    if (rights == 0) {
                        if (currentChat.megagroup && chatInfo != null && chatInfo.participants != null) {
                            boolean changed = false;
                            for (int a = 0; a < chatInfo.participants.participants.size(); a++) {
                                TLRPC.ChannelParticipant p = ((TLRPC.TL_chatChannelParticipant) chatInfo.participants.participants.get(a)).channelParticipant;
                                if (MessageObject.getPeerId(p.peer) == participant.user_id) {
                                    chatInfo.participants_count--;
                                    chatInfo.participants.participants.remove(a);
                                    changed = true;
                                    break;
                                }
                            }
                            if (chatInfo != null && chatInfo.participants != null) {
                                for (int a = 0; a < chatInfo.participants.participants.size(); a++) {
                                    TLRPC.ChatParticipant p = chatInfo.participants.participants.get(a);
                                    if (p.user_id == participant.user_id) {
                                        chatInfo.participants.participants.remove(a);
                                        changed = true;
                                        break;
                                    }
                                }
                            }
                            if (changed) {
                                updateOnlineCount(true);
                                updateRowsIds();
                                listAdapter.notifyDataSetChanged();
                            }
                        }
                    }
                }
            }

            @Override
            public void didChangeOwner(TLRPC.User user) {
                undoView.showWithAction(-chatId, currentChat.megagroup ? UndoView.ACTION_OWNER_TRANSFERED_GROUP : UndoView.ACTION_OWNER_TRANSFERED_CHANNEL, user);
            }
        });
        presentFragment(fragment);
    }

    private boolean processOnClickOrPress(final int position, final View view, final float x, final float y) {
        if (position == usernameRow || position == setUsernameRow) {
            final String username;
            final TLRPC.TL_username usernameObj;
            if (userId != 0) {
                final TLRPC.User user = getMessagesController().getUser(userId);
                String username1 = UserObject.getPublicUsername(user);
                if (user == null || username1 == null) {
                    return false;
                }
                username = username1;
                usernameObj = DialogObject.findUsername(username, user);
            } else if (chatId != 0) {
                final TLRPC.Chat chat = getMessagesController().getChat(chatId);
                if (chat == null || topicId == 0 && !ChatObject.isPublic(chat)) {
                    return false;
                }
                username = ChatObject.getPublicUsername(chat);
                usernameObj = DialogObject.findUsername(username, chat);
            } else {
                return false;
            }
            if (userId == 0) {
                TLRPC.Chat chat = getMessagesController().getChat(chatId);
                String link;
                if (ChatObject.isPublic(chat)) {
                    link = "https://" + getMessagesController().linkPrefix + "/" + ChatObject.getPublicUsername(chat) + (topicId != 0 ? "/" + topicId : "");
                } else {
                    link = "https://" + getMessagesController().linkPrefix + "/c/" + chat.id + (topicId != 0 ? "/" + topicId : "");
                }
                ShareAlert shareAlert = new ShareAlert(getParentActivity(), null, link, false, link, false) {
                    @Override
                    protected void onSend(LongSparseArray<TLRPC.Dialog> dids, int count, TLRPC.TL_forumTopic topic, boolean showToast) {
                        if (!showToast) return;
                        AndroidUtilities.runOnUIThread(() -> {
                            BulletinFactory.createInviteSentBulletin(getParentActivity(), contentView, dids.size(), dids.size() == 1 ? dids.valueAt(0).id : 0, count, getThemedColor(Theme.key_undo_background), getThemedColor(Theme.key_undo_infoColor)).show();
                        }, 250);
                    }
                };
                showDialog(shareAlert);
                if (usernameObj != null && !usernameObj.editable) {
                    TL_fragment.TL_getCollectibleInfo req = new TL_fragment.TL_getCollectibleInfo();
                    TL_fragment.TL_inputCollectibleUsername input = new TL_fragment.TL_inputCollectibleUsername();
                    input.username = usernameObj.username;
                    req.collectible = input;
                    int reqId = getConnectionsManager().sendRequest(req, (res, err) -> AndroidUtilities.runOnUIThread(() -> {
                        if (res instanceof TL_fragment.TL_collectibleInfo) {
                            TL_fragment.TL_collectibleInfo info = (TL_fragment.TL_collectibleInfo) res;
                            TLObject obj;
                            if (userId != 0) {
                                obj = getMessagesController().getUser(userId);
                            } else {
                                obj = getMessagesController().getChat(chatId);
                            }
                            final String usernameStr = "@" + usernameObj.username;
                            final String date = LocaleController.getInstance().getFormatterBoostExpired().format(new Date(info.purchase_date * 1000L));
                            final String cryptoAmount = BillingController.getInstance().formatCurrency(info.crypto_amount, info.crypto_currency);
                            final String amount = BillingController.getInstance().formatCurrency(info.amount, info.currency);
                            BulletinFactory.of(shareAlert.bulletinContainer2, resourcesProvider)
                                    .createImageBulletin(
                                            R.drawable.filled_username,
                                            AndroidUtilities.withLearnMore(AndroidUtilities.replaceTags(formatString(R.string.FragmentChannelUsername, usernameStr, date, cryptoAmount, TextUtils.isEmpty(amount) ? "" : "(" + amount + ")")), () -> {
                                                Bulletin.hideVisible();
                                                Browser.openUrl(getContext(), info.url);
                                            })
                                    )
                                    .setOnClickListener(v -> {
                                        Bulletin.hideVisible();
                                        Browser.openUrl(getContext(), info.url);
                                    })
                                    .show(false);
                        } else {
                            BulletinFactory.showError(err);
                        }
                    }));
                    getConnectionsManager().bindRequestToGuid(reqId, getClassGuid());
                }
            } else {
                if (editRow(view, position)) return true;

                if (usernameObj != null && !usernameObj.editable) {
                    TL_fragment.TL_getCollectibleInfo req = new TL_fragment.TL_getCollectibleInfo();
                    TL_fragment.TL_inputCollectibleUsername input = new TL_fragment.TL_inputCollectibleUsername();
                    input.username = usernameObj.username;
                    req.collectible = input;
                    int reqId = getConnectionsManager().sendRequest(req, (res, err) -> AndroidUtilities.runOnUIThread(() -> {
                        if (res instanceof TL_fragment.TL_collectibleInfo) {
                            TLObject obj;
                            if (userId != 0) {
                                obj = getMessagesController().getUser(userId);
                            } else {
                                obj = getMessagesController().getChat(chatId);
                            }
                            FragmentUsernameBottomSheet.open(getContext(), FragmentUsernameBottomSheet.TYPE_USERNAME, usernameObj.username, obj, (TL_fragment.TL_collectibleInfo) res, getResourceProvider());
                        } else {
                            BulletinFactory.showError(err);
                        }
                    }));
                    getConnectionsManager().bindRequestToGuid(reqId, getClassGuid());
                    return true;
                }

                try {
                    android.content.ClipboardManager clipboard = (android.content.ClipboardManager) ApplicationLoader.applicationContext.getSystemService(Context.CLIPBOARD_SERVICE);
                    String text = "@" + username;
                    BulletinFactory.of(this).createCopyBulletin(LocaleController.getString(R.string.UsernameCopied), resourcesProvider).show();
                    android.content.ClipData clip = android.content.ClipData.newPlainText("label", text);
                    clipboard.setPrimaryClip(clip);
                } catch (Exception e) {
                    FileLog.e(e);
                }
            }
            return true;
        } else if (position == noteRow) {

        } else if (position == phoneRow || position == numberRow) {
            if (editRow(view, position)) return true;

            final TLRPC.User user = getMessagesController().getUser(userId);
            if (user == null || user.phone == null || user.phone.length() == 0 || getParentActivity() == null) {
                return false;
            }

            if (position == phoneRow && user.phone.startsWith("888")) {
                final TL_fragment.TL_inputCollectiblePhone input = new TL_fragment.TL_inputCollectiblePhone();
                final String phone = input.phone = user.phone;
                final TL_fragment.TL_getCollectibleInfo req = new TL_fragment.TL_getCollectibleInfo();
                req.collectible = input;
                int reqId = getConnectionsManager().sendRequest(req, (res, err) -> AndroidUtilities.runOnUIThread(() -> {
                    if (res instanceof TL_fragment.TL_collectibleInfo) {
                        FragmentUsernameBottomSheet.open(getContext(), FragmentUsernameBottomSheet.TYPE_PHONE, phone, user, (TL_fragment.TL_collectibleInfo) res, getResourceProvider());
                    } else {
                        BulletinFactory.showError(err);
                    }
                }));
                getConnectionsManager().bindRequestToGuid(reqId, getClassGuid());
                return true;
            }

            final ItemOptions o = ItemOptions.makeOptions(this, view);
            o.setScrimViewBackground(listView.getClipBackground(view));
            if (position == phoneRow) {
                if (userInfo != null && userInfo.phone_calls_available) {
                    o.add(R.drawable.msg_calls, getString(R.string.CallViaTelegram), () -> {
                        if (getParentActivity() == null) return;
                        VoIPHelper.startCall(user, false, userInfo != null && userInfo.video_calls_available, getParentActivity(), userInfo, getAccountInstance());
                    });
                    if (userInfo.video_calls_available) {
                        o.add(R.drawable.msg_videocall, getString(R.string.VideoCallViaTelegram), () -> {
                            if (getParentActivity() == null) return;
                            VoIPHelper.startCall(user, true, userInfo != null && userInfo.video_calls_available, getParentActivity(), userInfo, getAccountInstance());
                        });
                    }
                }
                if (!isFragmentPhoneNumber) {
                    o.add(R.drawable.msg_calls_regular, getString(R.string.Call), () -> {
                        try {
                            Intent intent = new Intent(Intent.ACTION_DIAL, Uri.parse("tel:+" + user.phone));
                            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                            getParentActivity().startActivityForResult(intent, 500);
                        } catch (Exception e) {
                            FileLog.e(e);
                        }
                    });
                }
            }
            o.add(R.drawable.msg_copy, getString(R.string.Copy), () -> {
                try {
                    android.content.ClipboardManager clipboard = (android.content.ClipboardManager) ApplicationLoader.applicationContext.getSystemService(Context.CLIPBOARD_SERVICE);
                    android.content.ClipData clip = android.content.ClipData.newPlainText("label", "+" + user.phone);
                    clipboard.setPrimaryClip(clip);
                    if (AndroidUtilities.shouldShowClipboardToast()) {
                        BulletinFactory.of(this).createCopyBulletin(LocaleController.getString(R.string.PhoneCopied)).show();
                    }
                } catch (Exception e) {
                    FileLog.e(e);
                }
            });
            if (isFragmentPhoneNumber) {
                final SpannableStringBuilder spanned = new SpannableStringBuilder(AndroidUtilities.replaceTags(LocaleController.getString(R.string.AnonymousNumberNotice)));
                final int startIndex = TextUtils.indexOf(spanned, '*');
                final int lastIndex = TextUtils.lastIndexOf(spanned, '*');
                if (startIndex != -1 && lastIndex != -1 && startIndex != lastIndex) {
                    spanned.replace(lastIndex, lastIndex + 1, "");
                    spanned.replace(startIndex, startIndex + 1, "");
                    spanned.setSpan(new TypefaceSpan(AndroidUtilities.bold()), startIndex, lastIndex - 1, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                    spanned.setSpan(new ForegroundColorSpan(Theme.getColor(Theme.key_windowBackgroundWhiteValueText, resourcesProvider)), startIndex, lastIndex - 1, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                }
                o.addGap().addText(spanned, 13, dp(200));
                if (o.getLastView() instanceof TextView) {
                    final TextView textView = (TextView) o.getLastView();
                    textView.setBackground(Theme.createRadSelectorDrawable(Theme.getColor(Theme.key_dialogButtonSelector, resourcesProvider), 0, 6));
                    textView.setOnClickListener(v -> {
                        try {
                            v.getContext().startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("https://fragment.com")));
                        } catch (ActivityNotFoundException e) {
                            FileLog.e(e);
                        }
                    });
                }
            }
            o.show();
            return true;
        } else if (position == channelInfoRow || position == userInfoRow || position == locationRow || position == bioRow) {
            if (position == bioRow && (userInfo == null || TextUtils.isEmpty(userInfo.about))) {
                return false;
            }
            if (editRow(view, position)) return true;
            if (view instanceof AboutLinkCell && ((AboutLinkCell) view).onClick()) {
                return false;
            }
            String text;
            if (position == locationRow) {
                text = chatInfo != null && chatInfo.location instanceof TLRPC.TL_channelLocation ? ((TLRPC.TL_channelLocation) chatInfo.location).address : null;
            } else if (position == channelInfoRow) {
                text = chatInfo != null ? chatInfo.about : null;
            } else {
                text = userInfo != null ? userInfo.about : null;
            }
            final String finalText = text;
            if (TextUtils.isEmpty(finalText)) {
                return false;
            }
            final String[] fromLanguage = new String[1];
            fromLanguage[0] = "und";
            final boolean translateButtonEnabled = MessagesController.getInstance(currentAccount).getTranslateController().isContextTranslateEnabled();
            final boolean[] withTranslate = new boolean[1];
            withTranslate[0] = position == bioRow || position == channelInfoRow || position == userInfoRow;
            final String toLang = TranslateAlert2.getToLanguage();
            Runnable showMenu = () -> {
                if (getParentActivity() == null) {
                    return;
                }

                ItemOptions.makeOptions(this, view)
                    .setScrimViewBackground(listView.getClipBackground(view))
                    .add(R.drawable.msg_copy, getString(R.string.Copy), () -> {
                        AndroidUtilities.addToClipboard(finalText);
                        if (position == bioRow) {
                            BulletinFactory.of(this).createCopyBulletin(LocaleController.getString(R.string.BioCopied)).show();
                        } else {
                            BulletinFactory.of(this).createCopyBulletin(LocaleController.getString(R.string.TextCopied)).show();
                        }
                    })
                    .addIf(withTranslate[0], R.drawable.msg_translate, getString(R.string.TranslateMessage), () -> {
                        if (!AndroidUtilities.isContextSafe(getContext())) return;
                        TranslateAlert2.showAlert(getContext(), this, currentAccount, fromLanguage[0], toLang, finalText, null, false, span -> {
                            if (span != null) {
                                openUrl(span.getURL(), null);
                                return true;
                            }
                            return false;
                        }, null);
//                        new TranslateAlert3(getContext(), getResourceProvider())
//                            .setText(fromLanguage[0], finalText)
//                            .setToLanguage(toLang)
//                            .show();
                    })
                    .show();
            };
            if (withTranslate[0]) {
                if (LanguageDetector.hasSupport()) {
                    LanguageDetector.detectLanguage(finalText, (fromLang) -> {
                        fromLanguage[0] = fromLang;
                        withTranslate[0] = fromLang != null && (!fromLang.equals(toLang) || fromLang.equals("und")) && (
                                translateButtonEnabled && !RestrictedLanguagesSelectActivity.getRestrictedLanguages().contains(fromLang) ||
                                        (currentChat != null && (currentChat.has_link || ChatObject.isPublic(currentChat))) && ("uk".equals(fromLang) || "ru".equals(fromLang)));
                        showMenu.run();
                    }, (error) -> {
                        FileLog.e("mlkit: failed to detect language in selection", error);
                        showMenu.run();
                    });
                } else {
                    showMenu.run();
                }
            } else {
                showMenu.run();
            }
            return true;
        } else if (position == bizHoursRow || position == bizLocationRow) {
            if (getParentActivity() == null || userInfo == null) {
                return false;
            }
            final String finalText;
            if (position == bizHoursRow) {
                if (userInfo.business_work_hours == null) return false;
                finalText = OpeningHoursActivity.toString(currentAccount, userInfo.user, userInfo.business_work_hours);
            } else if (position == bizLocationRow) {
                if (editRow(view, position)) return true;
                if (userInfo.business_location == null) return false;
                finalText = userInfo.business_location.address;
            } else return true;

            AtomicReference<ActionBarPopupWindow> popupWindowRef = new AtomicReference<>();
            ActionBarPopupWindow.ActionBarPopupWindowLayout popupLayout = new ActionBarPopupWindow.ActionBarPopupWindowLayout(getContext(), R.drawable.popup_fixed_alert, resourcesProvider) {
                Path path = new Path();

                @Override
                protected boolean drawChild(Canvas canvas, View child, long drawingTime) {
                    canvas.save();
                    path.rewind();
                    AndroidUtilities.rectTmp.set(child.getLeft(), child.getTop(), child.getRight(), child.getBottom());
                    path.addRoundRect(AndroidUtilities.rectTmp, AndroidUtilities.dp(6), AndroidUtilities.dp(6), Path.Direction.CW);
                    canvas.clipPath(path);
                    boolean draw = super.drawChild(canvas, child, drawingTime);
                    canvas.restore();
                    return draw;
                }
            };
            popupLayout.setFitItems(true);

            ActionBarMenuItem.addItem(popupLayout, R.drawable.msg_copy, LocaleController.getString(R.string.Copy), false, resourcesProvider).setOnClickListener(v -> {
                popupWindowRef.get().dismiss();
                try {
                    AndroidUtilities.addToClipboard(finalText);
                    if (position == bizHoursRow) {
                        BulletinFactory.of(this).createCopyBulletin(LocaleController.getString(R.string.BusinessHoursCopied)).show();
                    } else {
                        BulletinFactory.of(this).createCopyBulletin(LocaleController.getString(R.string.BusinessLocationCopied)).show();
                    }
                } catch (Exception e) {
                    FileLog.e(e);
                }
            });

            ActionBarPopupWindow popupWindow = new ActionBarPopupWindow(popupLayout, LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT);
            popupWindow.setPauseNotifications(true);
            popupWindow.setDismissAnimationDuration(220);
            popupWindow.setOutsideTouchable(true);
            popupWindow.setClippingEnabled(true);
            popupWindow.setAnimationStyle(R.style.PopupContextAnimation);
            popupWindow.setFocusable(true);
            popupLayout.measure(View.MeasureSpec.makeMeasureSpec(AndroidUtilities.dp(1000), View.MeasureSpec.AT_MOST), View.MeasureSpec.makeMeasureSpec(AndroidUtilities.dp(1000), View.MeasureSpec.AT_MOST));
            popupWindow.setInputMethodMode(ActionBarPopupWindow.INPUT_METHOD_NOT_NEEDED);
            popupWindow.getContentView().setFocusableInTouchMode(true);
            popupWindowRef.set(popupWindow);

            float px = x, py = y;
            View v = view;
            while (v != null && v != getFragmentView()) {
                px += v.getX();
                py += v.getY();
                v = (View) v.getParent();
            }
            if (AndroidUtilities.isTablet()) {
                View pv = parentLayout.getView();
                if (pv != null) {
                    px += pv.getX() + pv.getPaddingLeft();
                    py += pv.getY() + pv.getPaddingTop();
                }
            }
            px -= popupLayout.getMeasuredWidth() / 2f;
            popupWindow.showAtLocation(getFragmentView(), 0, (int) px, (int) py);
            popupWindow.dimBehind();
            return true;
        }
        return false;
    }

    private void leaveChatPressed(boolean delete) {
        boolean isForum = ChatObject.isForum(currentChat);
        AlertsCreator.createClearOrDeleteDialogAlert(ProfileActivity.this, false, currentChat, null, false, isForum || delete || (currentChat != null && currentChat.creator), delete, !isForum, (param) -> {
            playProfileAnimation = 0;
            getNotificationCenter().removeObserver(ProfileActivity.this, NotificationCenter.closeChats);
            getNotificationCenter().postNotificationName(NotificationCenter.closeChats);
            finishFragment();
            getNotificationCenter().postNotificationName(NotificationCenter.needDeleteDialog, -currentChat.id, null, currentChat, param);
        });
    }

    private void getChannelParticipants(boolean reload) {
        if (loadingUsers || participantsMap == null || chatInfo == null) {
            return;
        }
        loadingUsers = true;
        final int delay = participantsMap.size() != 0 && reload ? 300 : 0;

        final TLRPC.TL_channels_getParticipants req = new TLRPC.TL_channels_getParticipants();
        req.channel = getMessagesController().getInputChannel(chatId);
        req.filter = new TLRPC.TL_channelParticipantsRecent();
        req.offset = reload ? 0 : participantsMap.size();
        req.limit = 200;
        int reqId = getConnectionsManager().sendRequest(req, (response, error) -> AndroidUtilities.runOnUIThread(() -> getNotificationCenter().doOnIdle(() -> {
            if (error == null) {
                TLRPC.TL_channels_channelParticipants res = (TLRPC.TL_channels_channelParticipants) response;
                getMessagesController().putUsers(res.users, false);
                getMessagesController().putChats(res.chats, false);
                if (res.users.size() < 200) {
                    usersEndReached = true;
                }
                if (req.offset == 0) {
                    participantsMap.clear();
                    chatInfo.participants = new TLRPC.TL_chatParticipants();
                    getMessagesStorage().putUsersAndChats(res.users, res.chats, true, true);
                    getMessagesStorage().updateChannelUsers(chatId, res.participants);
                }
                for (int a = 0; a < res.participants.size(); a++) {
                    TLRPC.TL_chatChannelParticipant participant = new TLRPC.TL_chatChannelParticipant();
                    participant.channelParticipant = res.participants.get(a);
                    participant.inviter_id = participant.channelParticipant.inviter_id;
                    participant.user_id = MessageObject.getPeerId(participant.channelParticipant.peer);
                    participant.date = participant.channelParticipant.date;
                    if (participantsMap.indexOfKey(participant.user_id) < 0) {
                        if (chatInfo.participants == null) {
                            chatInfo.participants = new TLRPC.TL_chatParticipants();
                        }
                        chatInfo.participants.participants.add(participant);
                        participantsMap.put(participant.user_id, participant);
                    }
                }
            }
            loadingUsers = false;
            saveScrollPosition();
            updateListAnimated(true);
        }), delay));
        getConnectionsManager().bindRequestToGuid(reqId, classGuid);
    }

    private AnimatorSet headerAnimatorSet;
    private AnimatorSet headerShadowAnimatorSet;
    private float mediaHeaderAnimationProgress;
    private boolean mediaHeaderVisible;
    private Property<ActionBar, Float> ACTIONBAR_HEADER_PROGRESS = new AnimationProperties.FloatProperty<ActionBar>("avatarAnimationProgress") {
        @Override
        public void setValue(ActionBar object, float value) {
            mediaHeaderAnimationProgress = value;
            if (storyView != null) {
                storyView.setActionBarActionMode(value);
            }
            if (giftsView != null) {
                giftsView.setActionBarActionMode(value);
            }
            topView.invalidate();

            int color1 = peerColor != null ? Color.WHITE : getThemedColor(Theme.key_profile_title);
            int color2 = getThemedColor(Theme.key_player_actionBarTitle);
            int c = AndroidUtilities.getOffsetColor(color1, color2, value, 1.0f);
            nameTextView[1].setTextColor(c);
            if (lockIconDrawable != null) {
                lockIconDrawable.setColorFilter(peerColor != null ? Color.WHITE : c, PorterDuff.Mode.MULTIPLY);
            }
            if (scamDrawable != null) {
                color1 = getThemedColor(Theme.key_avatar_subtitleInProfileBlue);
                scamDrawable.setColor(AndroidUtilities.getOffsetColor(color1, color2, value, 1.0f));
            }

            color1 = peerColor != null ? Color.WHITE : getThemedColor(Theme.key_actionBarDefaultIcon);
            color2 = getThemedColor(Theme.key_actionBarActionModeDefaultIcon);
            actionBar.setItemsColor(AndroidUtilities.getOffsetColor(color1, color2, value, 1.0f), false);

            color1 = peerColor != null ? Theme.ACTION_BAR_WHITE_SELECTOR_COLOR : peerColor != null ? 0x20ffffff : getThemedColor(Theme.key_avatar_actionBarSelectorBlue);
            color2 = getThemedColor(Theme.key_actionBarActionModeDefaultSelector);
            actionBar.setItemsBackgroundColor(AndroidUtilities.getOffsetColor(color1, color2, value, 1.0f), false);

            topView.invalidate();
            otherItem.setIconColor(peerColor != null ? Color.WHITE : getThemedColor(Theme.key_actionBarDefaultIcon));
            callItem.setIconColor(peerColor != null ? Color.WHITE : getThemedColor(Theme.key_actionBarDefaultIcon));
            videoCallItem.setIconColor(peerColor != null ? Color.WHITE : getThemedColor(Theme.key_actionBarDefaultIcon));
            editItem.setIconColor(peerColor != null ? Color.WHITE : getThemedColor(Theme.key_actionBarDefaultIcon));
            if (callToActionItem != null) {
                //callToActionItem.setIconColor(peerColor != null ? Color.WHITE : getThemedColor(Theme.key_actionBarDefaultIcon));
            }

            if (verifiedDrawable[0] != null) {
                color1 = getThemedColor(Theme.key_profile_verifiedBackground);
                color2 = getThemedColor(Theme.key_player_actionBarTitle);
                verifiedDrawable[0].setColorFilter(AndroidUtilities.getOffsetColor(color1, color2, value, 1.0f), PorterDuff.Mode.MULTIPLY);
            }
            if (verifiedDrawable[1] != null) {
                color1 = peerColor != null ? Theme.adaptHSV(ColorUtils.blendARGB(peerColor.getColor2(), peerColor.hasColor6(Theme.isCurrentThemeDark()) ? peerColor.getColor5() : peerColor.getColor3(), .4f), +.1f, Theme.isCurrentThemeDark() ? -.1f : -.08f) : getThemedColor(Theme.key_profile_verifiedBackground);
                color2 = getThemedColor(Theme.key_player_actionBarTitle);
                verifiedDrawable[1].setColorFilter(AndroidUtilities.getOffsetColor(color1, color2, value, 1.0f), PorterDuff.Mode.MULTIPLY);
            }

            if (verifiedCheckDrawable[0] != null) {
                color1 = getThemedColor(Theme.key_profile_verifiedCheck);
                color2 = getThemedColor(Theme.key_windowBackgroundWhite);
                verifiedCheckDrawable[0].setColorFilter(AndroidUtilities.getOffsetColor(color1, color2, value, 1.0f), PorterDuff.Mode.MULTIPLY);
            }
            if (verifiedCheckDrawable[1] != null) {
                color1 = peerColor != null ? Color.WHITE : dontApplyPeerColor(getThemedColor(Theme.key_profile_verifiedCheck));
                color2 = getThemedColor(Theme.key_windowBackgroundWhite);
                verifiedCheckDrawable[1].setColorFilter(AndroidUtilities.getOffsetColor(color1, color2, value, 1.0f), PorterDuff.Mode.MULTIPLY);
            }

            if (premiumStarDrawable[0] != null) {
                color1 = getThemedColor(Theme.key_profile_verifiedBackground);
                color2 = getThemedColor(Theme.key_player_actionBarTitle);
                premiumStarDrawable[0].setColorFilter(AndroidUtilities.getOffsetColor(color1, color2, value, 1.0f), PorterDuff.Mode.MULTIPLY);
            }
            if (premiumStarDrawable[1] != null) {
                color1 = dontApplyPeerColor(getThemedColor(Theme.key_profile_verifiedBackground));
                color2 = dontApplyPeerColor(getThemedColor(Theme.key_player_actionBarTitle));
                premiumStarDrawable[1].setColorFilter(AndroidUtilities.getOffsetColor(color1, color2, value, 1.0f), PorterDuff.Mode.MULTIPLY);
            }

            updateEmojiStatusDrawableColor();

            if (avatarsViewPagerIndicatorView.getSecondaryMenuItem() != null && (videoCallItemVisible || editItemVisible || callItemVisible)) {
                needLayoutText(calculateHeaderExtraDiff());
            }
        }

        @Override
        public Float get(ActionBar object) {
            return mediaHeaderAnimationProgress;
        }
    };

    private void setMediaHeaderVisible(boolean visible) {
        if (mediaHeaderVisible == visible) {
            return;
        }
        mediaHeaderVisible = visible;
        if (headerAnimatorSet != null) {
            headerAnimatorSet.cancel();
        }
        if (headerShadowAnimatorSet != null) {
            headerShadowAnimatorSet.cancel();
        }
        ActionBarMenuItem mediaSearchItem = sharedMediaLayout.getSearchItem();
        ImageView mediaOptionsItem = sharedMediaLayout.getSearchOptionsItem();
        TextView saveItem = sharedMediaLayout.getSaveItem();
        if (!mediaHeaderVisible) {
            if (callItemVisible) {
                callItem.setVisibility(View.VISIBLE);
            }
            if (videoCallItemVisible) {
                videoCallItem.setVisibility(View.VISIBLE);
            }
            if (editItemVisible) {
                editItem.setVisibility(View.VISIBLE);
            }
            otherItem.setVisibility(View.VISIBLE);
            if (mediaOptionsItem != null) {
                mediaOptionsItem.setVisibility(View.GONE);
            }
            if (saveItem != null) {
                saveItem.setVisibility(View.GONE);
            }
        } else {
            if (sharedMediaLayout.isSearchItemVisible()) {
                mediaSearchItem.setVisibility(View.VISIBLE);
            }
            if (mediaOptionsItem != null) {
                mediaOptionsItem.setVisibility(View.VISIBLE);
            }
            if (sharedMediaLayout.isOptionsItemVisible()) {
                sharedMediaLayout.photoVideoOptionsItem.setVisibility(View.VISIBLE);
                sharedMediaLayout.animateSearchToOptions(true, false);
            } else {
                sharedMediaLayout.photoVideoOptionsItem.setVisibility(View.INVISIBLE);
                sharedMediaLayout.animateSearchToOptions(false, false);
            }
        }
        updateStoriesViewBounds(false);

        if (actionBar != null) {
            actionBar.createMenu().requestLayout();
        }

        ArrayList<Animator> animators = new ArrayList<>();

        animators.add(ObjectAnimator.ofFloat(callItem, View.ALPHA, visible ? 0.0f : 1.0f));
        animators.add(ObjectAnimator.ofFloat(videoCallItem, View.ALPHA, visible ? 0.0f : 1.0f));
        animators.add(ObjectAnimator.ofFloat(otherItem, View.ALPHA, visible ? 0.0f : 1.0f));
        animators.add(ObjectAnimator.ofFloat(editItem, View.ALPHA, visible ? 0.0f : 1.0f));
        animators.add(ObjectAnimator.ofFloat(callItem, View.TRANSLATION_Y, visible ? -AndroidUtilities.dp(10) : 0.0f));
        animators.add(ObjectAnimator.ofFloat(videoCallItem, View.TRANSLATION_Y, visible ? -AndroidUtilities.dp(10) : 0.0f));
        animators.add(ObjectAnimator.ofFloat(otherItem, View.TRANSLATION_Y, visible ? -AndroidUtilities.dp(10) : 0.0f));
        animators.add(ObjectAnimator.ofFloat(editItem, View.TRANSLATION_Y, visible ? -AndroidUtilities.dp(10) : 0.0f));
        animators.add(ObjectAnimator.ofFloat(mediaSearchItem, View.ALPHA, visible ? 1.0f : 0.0f));
        animators.add(ObjectAnimator.ofFloat(mediaSearchItem, View.TRANSLATION_Y, visible ? 0.0f : AndroidUtilities.dp(10)));
        animators.add(ObjectAnimator.ofFloat(sharedMediaLayout.photoVideoOptionsItem, View.ALPHA, visible ? 1.0f : 0.0f));
        animators.add(ObjectAnimator.ofFloat(sharedMediaLayout.photoVideoOptionsItem, View.TRANSLATION_Y, visible ? 0.0f : AndroidUtilities.dp(10)));
        animators.add(ObjectAnimator.ofFloat(actionBar, ACTIONBAR_HEADER_PROGRESS, visible ? 1.0f : 0.0f));
        animators.add(ObjectAnimator.ofFloat(onlineTextView[1], View.ALPHA, visible ? 0.0f : 1.0f));
        sharedMediaLayout.scrollSlidingTextTabStrip.setOpen(visible);
        if (myProfile)
            animators.add(ObjectAnimator.ofFloat(onlineTextView[3], View.ALPHA, visible ? 0.0f : 1.0f));
        animators.add(ObjectAnimator.ofFloat(mediaCounterTextView, View.ALPHA, visible ? 1.0f : 0.0f));
        if (visible) {
            animators.add(ObjectAnimator.ofFloat(this, HEADER_SHADOW, 0.0f));
        }
        if (storyView != null || giftsView != null) {
            ValueAnimator va = ValueAnimator.ofFloat(0, 1);
            va.addUpdateListener(a -> updateStoriesViewBounds(true));
            animators.add(va);
        }

        checkStarRatingVisible();

        headerAnimatorSet = new AnimatorSet();
        headerAnimatorSet.playTogether(animators);
        headerAnimatorSet.setInterpolator(CubicBezierInterpolator.DEFAULT);
        headerAnimatorSet.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                if (headerAnimatorSet != null) {
                    if (mediaHeaderVisible) {
                        if (callItemVisible) {
                            callItem.setVisibility(View.GONE);
                        }
                        if (videoCallItemVisible) {
                            videoCallItem.setVisibility(View.GONE);
                        }
                        if (editItemVisible) {
                            editItem.setVisibility(View.GONE);
                        }
                        otherItem.setVisibility(View.GONE);
                    } else {
                        if (sharedMediaLayout.isSearchItemVisible()) {
                            mediaSearchItem.setVisibility(View.VISIBLE);
                        }

                        sharedMediaLayout.photoVideoOptionsItem.setVisibility(View.INVISIBLE);

                        headerShadowAnimatorSet = new AnimatorSet();
                        headerShadowAnimatorSet.playTogether(ObjectAnimator.ofFloat(ProfileActivity.this, HEADER_SHADOW, 1.0f));
                        headerShadowAnimatorSet.setDuration(100);
                        headerShadowAnimatorSet.addListener(new AnimatorListenerAdapter() {
                            @Override
                            public void onAnimationEnd(Animator animation) {
                                headerShadowAnimatorSet = null;
                            }
                        });
                        headerShadowAnimatorSet.start();
                    }
                }
                updateStoriesViewBounds(false);
                headerAnimatorSet = null;
            }

            @Override
            public void onAnimationCancel(Animator animation) {
                headerAnimatorSet = null;
            }
        });
        headerAnimatorSet.setDuration(150);
        headerAnimatorSet.start();
        NotificationCenter.getGlobalInstance().postNotificationName(NotificationCenter.needCheckSystemBarColors, true);
    }

    private void openAddMember() {
        Bundle args = new Bundle();
        args.putBoolean("addToGroup", true);
        args.putLong("chatId", currentChat.id);
        GroupCreateActivity fragment = new GroupCreateActivity(args);
        fragment.setInfo(chatInfo);
        if (chatInfo != null && chatInfo.participants != null) {
            LongSparseArray<TLObject> users = new LongSparseArray<>();
            for (int a = 0; a < chatInfo.participants.participants.size(); a++) {
                users.put(chatInfo.participants.participants.get(a).user_id, null);
            }
            fragment.setIgnoreUsers(users);
        }
        fragment.setDelegate2((users, fwdCount) -> {
            HashSet<Long> currentParticipants = new HashSet<>();
            ArrayList<TLRPC.User> addedUsers = new ArrayList<>();
            if (chatInfo != null && chatInfo.participants != null && chatInfo.participants.participants != null) {
                for (int i = 0; i < chatInfo.participants.participants.size(); i++) {
                    currentParticipants.add(chatInfo.participants.participants.get(i).user_id);
                }
            }
            getMessagesController().addUsersToChat(currentChat, ProfileActivity.this, users, fwdCount, user -> {
                addedUsers.add(user);
            }, restrictedUser -> {
                for (int i = 0; i < chatInfo.participants.participants.size(); i++) {
                    if (chatInfo.participants.participants.get(i).user_id == restrictedUser.id) {
                        chatInfo.participants.participants.remove(i);
                        updateListAnimated(true);
                        break;
                    }
                }
            }, () -> {
                int N = addedUsers.size();
                int[] finished = new int[1];
                for (int a = 0; a < N; a++) {
                    TLRPC.User user = addedUsers.get(a);
                    if (!currentParticipants.contains(user.id)) {
                        if (chatInfo.participants == null) {
                            chatInfo.participants = new TLRPC.TL_chatParticipants();
                        }
                        if (ChatObject.isChannel(currentChat)) {
                            TLRPC.TL_chatChannelParticipant channelParticipant1 = new TLRPC.TL_chatChannelParticipant();
                            channelParticipant1.channelParticipant = new TLRPC.TL_channelParticipant();
                            channelParticipant1.channelParticipant.inviter_id = getUserConfig().getClientUserId();
                            channelParticipant1.channelParticipant.peer = new TLRPC.TL_peerUser();
                            channelParticipant1.channelParticipant.peer.user_id = user.id;
                            channelParticipant1.channelParticipant.date = getConnectionsManager().getCurrentTime();
                            channelParticipant1.user_id = user.id;
                            chatInfo.participants.participants.add(channelParticipant1);
                        } else {
                            TLRPC.ChatParticipant participant = new TLRPC.TL_chatParticipant();
                            participant.user_id = user.id;
                            participant.inviter_id = getAccountInstance().getUserConfig().clientUserId;
                            chatInfo.participants.participants.add(participant);
                        }
                        chatInfo.participants_count++;
                        getMessagesController().putUser(user, false);
                    }
                }
                updateListAnimated(true);
            });

        });
        presentFragment(fragment);
    }

    private void checkListViewScroll() {
        if (listView.getVisibility() != View.VISIBLE) {
            return;
        }
        if (sharedMediaLayoutAttached) {
            sharedMediaLayout.setVisibleHeight(listView.getMeasuredHeight() - sharedMediaLayout.getTop());
        }

        if (listView.getChildCount() <= 0 || openAnimationInProgress) {
            return;
        }

        int newOffset = 0;
        View child = null;
        for (int i = 0; i < listView.getChildCount(); i++) {
            if (listView.getChildAdapterPosition(listView.getChildAt(i)) == 0) {
                child = listView.getChildAt(i);
                break;
            }
        }
        RecyclerListView.Holder holder = child == null ? null : (RecyclerListView.Holder) listView.findContainingViewHolder(child);
        int top = child == null ? 0 : child.getTop();
        int adapterPosition = holder != null ? holder.getAdapterPosition() : RecyclerView.NO_POSITION;
        if (top >= 0 && adapterPosition == 0) {
            newOffset = top;
        }
        boolean mediaHeaderVisible;
        boolean searchVisible = imageUpdater == null && actionBar.isSearchFieldVisible();
        if (sharedMediaRow != -1 && !searchVisible) {
            holder = (RecyclerListView.Holder) listView.findViewHolderForAdapterPosition(sharedMediaRow);
            mediaHeaderVisible = holder != null && holder.itemView.getTop() <= 0;
        } else {
            mediaHeaderVisible = searchVisible;
        }
        setMediaHeaderVisible(mediaHeaderVisible);

        if (extraHeight != newOffset && !transitionAnimationInProress) {
            extraHeight = newOffset;
            topView.invalidate();
            if (playProfileAnimation != 0) {
                allowProfileAnimation = extraHeight > getActionsExtraHeight();
            }
            needLayout(true);
        }
    }

    public void updateSelectedMediaTabText() {
        if (sharedMediaLayout == null || mediaCounterTextView == null) {
            return;
        }
        int id = sharedMediaLayout.getClosestTab();
        int[] mediaCount = sharedMediaPreloader.getLastMediaCount();
        if (id == SharedMediaLayout.TAB_PHOTOVIDEO) {
            if (mediaCount[MediaDataController.MEDIA_VIDEOS_ONLY] <= 0 && mediaCount[MediaDataController.MEDIA_PHOTOS_ONLY] <= 0) {
                if (mediaCount[MediaDataController.MEDIA_PHOTOVIDEO] <= 0) {
                    mediaCounterTextView.setText(LocaleController.getString(R.string.SharedMedia));
                } else {
                    mediaCounterTextView.setText(LocaleController.formatPluralString("Media", mediaCount[MediaDataController.MEDIA_PHOTOVIDEO]));
                }
            } else if (sharedMediaLayout.getPhotosVideosTypeFilter() == SharedMediaLayout.FILTER_PHOTOS_ONLY || mediaCount[MediaDataController.MEDIA_VIDEOS_ONLY] <= 0) {
                mediaCounterTextView.setText(LocaleController.formatPluralString("Photos", mediaCount[MediaDataController.MEDIA_PHOTOS_ONLY]));
            } else if (sharedMediaLayout.getPhotosVideosTypeFilter() == SharedMediaLayout.FILTER_VIDEOS_ONLY || mediaCount[MediaDataController.MEDIA_PHOTOS_ONLY] <= 0) {
                mediaCounterTextView.setText(LocaleController.formatPluralString("Videos", mediaCount[MediaDataController.MEDIA_VIDEOS_ONLY]));
            } else {
                String str = String.format("%s, %s", LocaleController.formatPluralString("Photos", mediaCount[MediaDataController.MEDIA_PHOTOS_ONLY]), LocaleController.formatPluralString("Videos", mediaCount[MediaDataController.MEDIA_VIDEOS_ONLY]));
                mediaCounterTextView.setText(str);
            }
        } else if (id == SharedMediaLayout.TAB_FILES) {
            if (mediaCount[MediaDataController.MEDIA_FILE] <= 0) {
                mediaCounterTextView.setText(LocaleController.getString(R.string.Files));
            } else {
                mediaCounterTextView.setText(LocaleController.formatPluralString("Files", mediaCount[MediaDataController.MEDIA_FILE]));
            }
        } else if (id == SharedMediaLayout.TAB_VOICE) {
            if (mediaCount[MediaDataController.MEDIA_AUDIO] <= 0) {
                mediaCounterTextView.setText(LocaleController.getString(R.string.Voice));
            } else {
                mediaCounterTextView.setText(LocaleController.formatPluralString("Voice", mediaCount[MediaDataController.MEDIA_AUDIO]));
            }
        } else if (id == SharedMediaLayout.TAB_LINKS) {
            if (mediaCount[MediaDataController.MEDIA_URL] <= 0) {
                mediaCounterTextView.setText(LocaleController.getString(R.string.SharedLinks));
            } else {
                mediaCounterTextView.setText(LocaleController.formatPluralString("Links", mediaCount[MediaDataController.MEDIA_URL]));
            }
        } else if (id == SharedMediaLayout.TAB_AUDIO) {
            if (mediaCount[MediaDataController.MEDIA_MUSIC] <= 0) {
                mediaCounterTextView.setText(LocaleController.getString(R.string.Music));
            } else {
                mediaCounterTextView.setText(LocaleController.formatPluralString("MusicFiles", mediaCount[MediaDataController.MEDIA_MUSIC]));
            }
        } else if (id == SharedMediaLayout.TAB_GIF) {
            if (mediaCount[MediaDataController.MEDIA_GIF] <= 0) {
                mediaCounterTextView.setText(LocaleController.getString(R.string.AccDescrGIFs));
            } else {
                mediaCounterTextView.setText(LocaleController.formatPluralString("GIFs", mediaCount[MediaDataController.MEDIA_GIF]));
            }
        } else if (id == SharedMediaLayout.TAB_COMMON_GROUPS) {
            mediaCounterTextView.setText(LocaleController.formatPluralString("CommonGroups", userInfo.common_chats_count));
        } else if (id == SharedMediaLayout.TAB_GROUPUSERS) {
            mediaCounterTextView.setText(onlineTextView[1].getText());
        } else if (id == SharedMediaLayout.TAB_STORIES || SharedMediaLayout.isStoryAlbumPageType(id)) {
            if (isBot) {
                mediaCounterTextView.setText(sharedMediaLayout.getBotPreviewsSubtitle(false));
            } else {
                int count = sharedMediaLayout.getStoriesCount(id);
                if (count > 0) {
                    mediaCounterTextView.setText(LocaleController.formatPluralString("ProfileStoriesCount", sharedMediaLayout.getStoriesCount(id)));
                } else {
                    mediaCounterTextView.setText(getString(R.string.ProfileStoriesCountZero));
                }
            }
        } else if (id == SharedMediaLayout.TAB_BOT_PREVIEWS) {
            mediaCounterTextView.setText(sharedMediaLayout.getBotPreviewsSubtitle(true));
        } else if (id == SharedMediaLayout.TAB_ARCHIVED_STORIES) {
            mediaCounterTextView.setText(LocaleController.formatPluralString("ProfileStoriesArchiveCount", sharedMediaLayout.getStoriesCount(id)));
        } else if (id == SharedMediaLayout.TAB_RECOMMENDED_CHANNELS) {
            final MessagesController.ChannelRecommendations rec = MessagesController.getInstance(currentAccount).getChannelRecommendations(getDialogId());
            mediaCounterTextView.setText(LocaleController.formatPluralString(isBot ? "Bots" : "Channels", rec == null ? 0 : rec.chats.size() + rec.more));
        } else if (id == SharedMediaLayout.TAB_SAVED_MESSAGES) {
            int messagesCount = getMessagesController().getSavedMessagesController().getMessagesCount(getDialogId());
            mediaCounterTextView.setText(LocaleController.formatPluralString("SavedMessagesCount", Math.max(1, messagesCount)));
        } else if (id == SharedMediaLayout.TAB_GIFTS) {
            mediaCounterTextView.setText(LocaleController.formatPluralStringComma("ProfileGiftsCount", sharedMediaLayout.giftsContainer == null ? 0 : sharedMediaLayout.giftsContainer.getGiftsCount()));
        } else if (id == SharedMediaLayout.TAB_POLL) {
            if (mediaCount[MediaDataController.MEDIA_POLL] <= 0) {
                mediaCounterTextView.setText(LocaleController.getString(R.string.SharedPollTab));
            } else {
                mediaCounterTextView.setText(LocaleController.formatPluralStringComma("ProfilePollsCount", mediaCount[MediaDataController.MEDIA_POLL]));
            }
        }
    }
    private boolean isStarRatingVisible1;

    private void checkStarRatingVisible() {
        if (ratingView != null) {
            ratingView.setVisibility(!mediaHeaderVisible && isStarRatingVisible1);
        }
    }

    private Animator.AnimatorListener resetListener = new AnimatorListenerAdapter() {
        @Override
        public void onAnimationEnd(Animator animation) {
            super.onAnimationEnd(animation);
            needLayout(true);
        }
    };

    private float[] backwardInitialValues = null;

    private void captureBackwardInitialValues() {
        if (backwardInitialValues == null) {
            backwardInitialValues = new float[16];
        }

        backwardTransitionFromExtraHeight = extraHeight;
        backwardInitialValues[0] = avatarContainer.getScaleX();
        backwardInitialValues[1] = avatarContainer.getTranslationX();
        backwardInitialValues[2] = avatarContainer.getTranslationY();
        if (actionsView != null) {
            backwardInitialValues[3] = actionsView.getTranslationY();
        }
        if (giftsView != null) {
            backwardInitialValues[4] = giftsView.expandProgress;
            backwardInitialValues[5] = giftsView.collapseProgress;
        }
        if (showStatusButton != null) {
            backwardInitialValues[6] = showStatusButton.getAlpha();
        }
        backwardInitialValues[7] = nameTextView[1].getScaleX();
        backwardInitialValues[8] = nameTextView[1].getTranslationY();
        backwardInitialValues[9] = onlineTextView[1].getTranslationY();
        backwardInitialValues[10] = nameTextView[1].getLayoutParams().width;
        backwardInitialValues[11] = pullUpProgress;
//        metaball.captureBackward();

        for (int a = 0; a < nameTextView.length; a++) {
            if (nameTextView[a] == null) {
                continue;
            }
            backwardInitialValues[12 + a * 2] = nameTextView[a].getTranslationX();
            backwardInitialValues[12 + a * 2 + 1] = onlineTextView[a].getTranslationX();
        }
    }

    private void backwardAnimationLayout() {
        if (backwardInitialValues == null) return;

        if (expandAnimator != null && expandAnimator.isRunning()) {
            expandAnimator.cancel();
        }

        final float backwardDiff = Utilities.clamp01(extraHeight / backwardTransitionFromExtraHeight);
        if (backwardDiff <= 0) {
            return;
        }
        final float backwardDiffHalf = (backwardDiff - 0.5f) / 0.5f;

        float startAvatarY =
            (actionBar.getOccupyStatusBar() ? AndroidUtilities.statusBarHeight : 0)
                + ActionBar.getCurrentActionBarHeight() / 2f
                - dp(21)
                + actionBar.getTranslationY();

        avatarScale = lerp(42f / 100f, backwardInitialValues[0], backwardDiff);
        avatarX = lerp(0, backwardInitialValues[1], backwardDiff);
        avatarY = lerp(startAvatarY, backwardInitialValues[2], backwardDiff);
        pullUpProgress = lerp(backwardInitialValues[11], 0f, backwardDiff);
        avatarContainer.setScaleX(avatarScale);
        avatarContainer.setScaleY(avatarScale);
        avatarContainer.setTranslationX(avatarX);
        avatarContainer.setTranslationY(avatarY);
        if (ratingView != null) {
            ratingView.setAlpha(backwardDiff);
        }

//        if (metaball != null && metaball.isBackward) {
//            metaball.updateBackward(backwardDiff);
//        } else {
            avatarImage.setAlpha(1f);
            avatarContainer.setAlpha(1f);
//        }

        if (storyView != null) {
            storyView.invalidate();
        }
        if (giftsView != null) {
            giftsView.expandProgress = AndroidUtilities.lerp(0f, backwardInitialValues[4], backwardDiffHalf);
            giftsView.collapseProgress = AndroidUtilities.lerp(0f, backwardInitialValues[5], backwardDiff);
            giftsView.isOpening = true;
            giftsView.invalidate();
        }

        if (showStatusButton != null) {
            showStatusButton.setAlpha((int) AndroidUtilities.lerp(0, backwardInitialValues[6], backwardDiff));
        }

        float extra = dp(42) * (avatarScale * 100 / 42) - dp(42);
        timeItem.setTranslationX(avatarContainer.getX() + dp(16) + extra);
        timeItem.setTranslationY(avatarContainer.getY() + dp(15) + extra);
        starBgItem.setTranslationX(avatarContainer.getX() + dp(28) + extra);
        starBgItem.setTranslationY(avatarContainer.getY() + dp(24) + extra);
        starFgItem.setTranslationX(avatarContainer.getX() + dp(28) + extra);
        starFgItem.setTranslationY(avatarContainer.getY() + dp(24) + extra);

        float nameScale = AndroidUtilities.lerp(1f, backwardInitialValues[7], backwardDiff);

        final float minEndNameY = (actionBar.getOccupyStatusBar() ? AndroidUtilities.statusBarHeight : 0)
                + ActionBar.getCurrentActionBarHeight() / 2f
                - 21 * AndroidUtilities.density
                + actionBar.getTranslationY();

        final float minNameY = (float) (Math.floor(minEndNameY)) + AndroidUtilities.dp(1.3f);
        final float minOnlineY = minNameY + AndroidUtilities.dpf2(22.7f);

        nameY = AndroidUtilities.lerp(minNameY, backwardInitialValues[8], backwardDiff);
        onlineY = AndroidUtilities.lerp(minOnlineY, backwardInitialValues[9], backwardDiff);

        for (int a = 0; a < nameTextView.length; a++) {
            if (nameTextView[a] == null) {
                continue;
            }

            float nameX = lerp(0f, backwardInitialValues[12 + a * 2], backwardDiff);
            float onlineX = lerp(0f, backwardInitialValues[12 + a * 2 + 1], backwardDiff);

            nameTextView[a].setTranslationX(nameX);
            nameTextView[a].setTranslationY(nameY);
            onlineTextView[a].setTranslationX(onlineX + customPhotoOffset);
            onlineTextView[a].setTranslationY(onlineY);
            if (a == 1) {
                this.nameX = nameX;
                this.onlineX = onlineX;
                mediaCounterTextView.setTranslationX(onlineX);
                mediaCounterTextView.setTranslationY(onlineY);
            }
            nameTextView[a].setScaleX(nameScale);
            nameTextView[a].setScaleY(nameScale);
        }
        updateCollectibleHint();

        final int newTop = (actionBar.getOccupyStatusBar() ? AndroidUtilities.statusBarHeight : 0) + ActionBar.getCurrentActionBarHeight();
        updateExtraViews(newTop);

        needLayoutText(backwardDiff, (int) backwardInitialValues[10], true);

        if (actionsView != null) {
            actionsView.setAlpha(AndroidUtilities.lerp(0, backwardInitialValues[3], backwardDiffHalf));
        }
    }

    private void updateWriteButtonLayout(boolean animated) {
        float diff = calculateHeaderExtraDiff();
        boolean writeButtonVisible = false;

        isStarRatingVisible1 = diff > 0.2f && !searchMode && (imageUpdater == null || setAvatarRow == -1);
        checkStarRatingVisible();
        if (writeButtonVisible && chatId != 0) {
            writeButtonVisible = ChatObject.isChannel(currentChat) && !currentChat.megagroup && chatInfo != null && chatInfo.linked_chat_id != 0 && infoHeaderRow != -1;
        }

        if (writeButton != null && writeButton.getVisibility() != View.GONE) {
            writeButton.setTranslationY((actionBar.getOccupyStatusBar() ? AndroidUtilities.statusBarHeight : 0) + ActionBar.getCurrentActionBarHeight() + extraHeight + searchTransitionOffset - AndroidUtilities.dp(29.5f));

            writeButtonVisible = diff > 0.2f && !searchMode && !myProfile && (imageUpdater == null || setAvatarRow == -1);
            if (writeButtonVisible && chatId != 0) {
                writeButtonVisible = ChatObject.isChannel(currentChat) && !currentChat.megagroup && chatInfo != null && chatInfo.linked_chat_id != 0 && (infoHeaderRow != -1 || infoHeaderRowEmpty != -1);
            }
            if (!openAnimationInProgress) {
                boolean currentVisible = writeButton.getTag() == null;
                if (writeButtonVisible != currentVisible) {
                    if (writeButtonVisible) {
                        writeButton.setTag(null);
                    } else {
                        writeButton.setTag(0);
                    }
                    if (writeButtonAnimation != null) {
                        AnimatorSet old = writeButtonAnimation;
                        writeButtonAnimation = null;
                        old.cancel();
                    }
                    if (animated) {
                        writeButtonAnimation = new AnimatorSet();
                        if (writeButtonVisible) {
                            writeButtonAnimation.setInterpolator(new DecelerateInterpolator());
                            writeButtonAnimation.playTogether(
                                    ObjectAnimator.ofFloat(writeButton, View.SCALE_X, 1.0f),
                                    ObjectAnimator.ofFloat(writeButton, View.SCALE_Y, 1.0f),
                                    ObjectAnimator.ofFloat(writeButton, View.ALPHA, 1.0f)
                            );
                        } else {
                            writeButtonAnimation.setInterpolator(new AccelerateInterpolator());
                            writeButtonAnimation.playTogether(
                                    ObjectAnimator.ofFloat(writeButton, View.SCALE_X, 0.2f),
                                    ObjectAnimator.ofFloat(writeButton, View.SCALE_Y, 0.2f),
                                    ObjectAnimator.ofFloat(writeButton, View.ALPHA, 0.0f)
                            );
                        }
                        writeButtonAnimation.setDuration(150);
                        writeButtonAnimation.addListener(new AnimatorListenerAdapter() {
                            @Override
                            public void onAnimationEnd(Animator animation) {
                                if (writeButtonAnimation != null && writeButtonAnimation.equals(animation)) {
                                    writeButtonAnimation = null;
                                }
                            }
                        });
                        writeButtonAnimation.start();
                    } else {
                        writeButton.setScaleX(writeButtonVisible ? 1.0f : 0.2f);
                        writeButton.setScaleY(writeButtonVisible ? 1.0f : 0.2f);
                        writeButton.setAlpha(writeButtonVisible ? 1.0f : 0.0f);
                    }
                }
            }
        }
        if (storyView != null) {
            storyView.setExpandCoords(avatarContainer2.getMeasuredWidth() - AndroidUtilities.dp(40), writeButtonVisible, (actionBar.getOccupyStatusBar() ? AndroidUtilities.statusBarHeight : 0) + ActionBar.getCurrentActionBarHeight() / 2.0f/* + extraHeight + searchTransitionOffset*/);
        }
        if (giftsView != null) {
            giftsView.setExpandCoords((actionBar.getOccupyStatusBar() ? AndroidUtilities.statusBarHeight : 0) + ActionBar.getCurrentActionBarHeight() + extraHeight + searchTransitionOffset);
        }
    }

    private void needLayout(boolean animated) {
        final int newTop = (actionBar.getOccupyStatusBar() ? AndroidUtilities.statusBarHeight : 0) + ActionBar.getCurrentActionBarHeight();
        final float diff = calculateHeaderExtraDiff();
        boolean textMeasured = false;

        FrameLayout.LayoutParams layoutParams;
        if (listView != null && !openAnimationInProgress) {
            layoutParams = (FrameLayout.LayoutParams) listView.getLayoutParams();
            if (layoutParams.topMargin != newTop) {
                layoutParams.topMargin = newTop;
                listView.setLayoutParams(layoutParams);
            }
        }

        updateWriteButtonLayout(animated);

        if (avatarContainer != null) {
            listView.setTopGlowOffset((int) extraHeight);
            listView.setOverScrollMode(extraHeight > getHeaderExtraHeight() && extraHeight < listView.getMeasuredWidth() + getActionsExtraHeight() - newTop ? View.OVER_SCROLL_NEVER : View.OVER_SCROLL_ALWAYS);
        }

        if (avatarContainer != null && !isFragmentOpened && openAnimationInProgress) {
            backwardAnimationLayout();
            return;
        }

        final int headerExtraHeight = getHeaderExtraHeight();// - dp(hasMusic ? 25 : 0);

        if (avatarContainer != null) {
            if (actionsView != null) {
                if (chatId != 0) {
                    boolean discuss = ChatObject.isChannel(currentChat) && !currentChat.megagroup && chatInfo != null && chatInfo.linked_chat_id != 0;
                    actionsView.set(ProfileActionsView.KEY_DISCUSS, discuss);
                }
            }

            updateWriteButtonLayout(animated);

            float endNameY =
                (actionBar.getOccupyStatusBar() ? AndroidUtilities.statusBarHeight : 0)
                + ActionBar.getCurrentActionBarHeight() / 2f * (1f + diff)
                - dp(21)
                + actionBar.getTranslationY();

            if (!openAnimationInProgress) {
                float endY = -dp(29);
                if (avatarGooey != null && avatarGooey.notchInfo != null && avatarGooey.notchInfo.isLikelyCircle) {
                    endY = avatarGooey.notchInfo.bounds.centerY();
                }
                float startY =
                    (actionBar.getOccupyStatusBar() ? AndroidUtilities.statusBarHeight : 0)
                        + ActionBar.getCurrentActionBarHeight()
                        - dp(21)
                        + actionBar.getTranslationY();
                avatarY = lerp(endY, startY, diff);
            } else {
                avatarY = endNameY;
            }

            float h = openAnimationInProgress ? initialAnimationExtraHeight : extraHeight;
            if (h > headerExtraHeight || isPulledDown) {
                expandProgress = Math.max(0f, Math.min(1f, (h - headerExtraHeight) / (listView.getMeasuredWidth() - newTop - getHeaderOnlyExtraHeight())));
                avatarScale = lerp(96 / 42f, 138 / 42f, Math.min(1f, expandProgress * 3f)) / 100f * 42f;
                pullUpProgress = 0.0f;

                if (storyView != null) {
                    storyView.invalidate();
                }
                if (giftsView != null) {
                    giftsView.invalidate();
                }

                final float durationFactor = Math.min(AndroidUtilities.dpf2(2000f), Math.max(AndroidUtilities.dpf2(1100f), Math.abs(listViewVelocityY))) / AndroidUtilities.dpf2(1100f);
                final boolean hasActions = getActionsExtraHeight() > 0;

                if (allowPullingDown && (openingAvatar || expandProgress >= 0.33f)) {
                    if (!isPulledDown) {
                        if (otherItem != null) {
                            if (!isPeerNoForwards()) {
                                otherItem.showSubItem(gallery_menu_save);
                            } else {
                                otherItem.hideSubItem(gallery_menu_save);
                            }
                            if (imageUpdater != null) {
                                otherItem.showSubItem(add_photo);
                                otherItem.showSubItem(edit_avatar);
                                otherItem.showSubItem(delete_avatar);
                                otherItem.hideSubItem(set_as_main);
                                otherItem.hideSubItem(logout);
                            }
                        }
                        if (searchItem != null) {
                            searchItem.setEnabled(false);
                        }
                        isPulledDown = true;
                        NotificationCenter.getGlobalInstance().postNotificationName(NotificationCenter.needCheckSystemBarColors, true);
                        overlaysView.setOverlaysVisible(true, durationFactor);
                        avatarsViewPagerIndicatorView.refreshVisibility(durationFactor);
                        avatarsViewPager.setCreateThumbFromParent(true);
                        avatarsViewPager.getAdapter().notifyDataSetChanged();
                        expandAnimator.cancel();
                        float value = AndroidUtilities.lerp(expandAnimatorValues, currentExpanAnimatorFracture);
                        expandAnimatorValues[0] = value;
                        expandAnimatorValues[1] = 1f;
                        if (storyView != null && !storyView.isEmpty()) {
                            expandAnimator.setInterpolator(new FastOutSlowInInterpolator());
                            expandAnimator.setDuration((long) ((1f - value) * 1.3f * 250f / durationFactor));
                        } else {
                            expandAnimator.setInterpolator(CubicBezierInterpolator.EASE_BOTH);
                            expandAnimator.setDuration((long) ((1f - value) * 250f / durationFactor));
                        }
                        expandAnimator.addListener(new AnimatorListenerAdapter() {
                            @Override
                            public void onAnimationStart(Animator animation) {
                                setForegroundImage(false);
                                avatarsViewPager.setAnimatedFileMaybe(avatarImage.getImageReceiver().getAnimation());
                                avatarsViewPager.resetCurrentItem();
                            }

                            @Override
                            public void onAnimationEnd(Animator animation) {
                                justFullyExpanded = false;
                                listView.canStopFlinger = true;
                                expandAnimator.removeListener(this);
                                topView.setBackgroundColor(Color.BLACK);
                                avatarContainer.setVisibility(View.GONE);
                                avatarsViewPager.setVisibility(View.VISIBLE);
                                avatarsViewPager.setAlpha(1f);
                            }

                            @Override
                            public void onAnimationCancel(Animator animation) {
                                justFullyExpanded = false;
                                listView.canStopFlinger = true;
                            }
                        });
                        final View view = layoutManager.findViewByPosition(0);
                        if (!ignoreScrollOnFullExpand && view != null /*&& hasActions*/) {
                            justFullyExpanded = true;
                            final int actionBarHeight = ActionBar.getCurrentActionBarHeight() + (actionBar.getOccupyStatusBar() ? AndroidUtilities.statusBarHeight : 0);
                            listView.smoothScrollBy(0, view.getTop() - listView.getMeasuredWidth() - getActionsExtraHeight() + actionBarHeight, (int) expandAnimator.getDuration(), (Interpolator) expandAnimator.getInterpolator());
                            listView.canStopFlinger = false;
                        }
                        expandNameYStartedFrom = nameTextView[1].getTranslationY();
                        expandOnlineYStartedFrom = onlineTextView[1].getTranslationY();
                        expandAnimator.start();
                        avatarsViewPager.setAlpha(0f);
                        avatarsViewPager.setVisibility(View.VISIBLE);

                        try {
                            avatarContainer.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS, HapticFeedbackConstants.FLAG_IGNORE_VIEW_SETTING);
                        } catch (Exception ignore) {
                        }
                    }
                    ViewGroup.LayoutParams params = avatarsViewPager.getLayoutParams();
                    int oldHeight = params.height;
                    params.width = listView.getMeasuredWidth();
                    params.height = /*hasActions && avatarsViewPager.getAlpha() <= 0f ? params.width + getActionsExtraHeight() :*/ (int) (h + newTop);
                    if (oldHeight != params.height) {
                        avatarsViewPager.requestLayout();
                    }
                    if (!expandAnimator.isRunning()) {
                        float additionalTranslationY = 0;
                        if (openAnimationInProgress && playProfileAnimation == 2) {
                            additionalTranslationY = -(1.0f - avatarAnimationProgress) * dp(50);
                        }
                        nameTextView[1].setTranslationX(AndroidUtilities.dpf2(18f) - nameTextView[1].getLeft());
                        nameTextView[1].setTranslationY(newTop + h - getActionsExtraHeight() - dpf2(30f) - nameTextView[1].getBottom() + additionalTranslationY);
                        onlineTextView[1].setTranslationX(getOnlineTextViewTranslationXWithOffsets(dpf2(16) - onlineTextView[1].getLeft()));
                        onlineTextView[1].setTranslationY(getOnlineTextViewTranslationYWithOffsets(newTop + h - getActionsExtraHeight() - dpf2(10) - onlineTextView[1].getBottom() + additionalTranslationY));
                        mediaCounterTextView.setTranslationX(onlineTextView[1].getTranslationX());
                        mediaCounterTextView.setTranslationY(onlineTextView[1].getTranslationY());
                        updateCollectibleHint();
                    }
                } else {
                    if (isPulledDown) {
                        isPulledDown = false;
                        NotificationCenter.getGlobalInstance().postNotificationName(NotificationCenter.needCheckSystemBarColors, true);
                        if (otherItem != null) {
                            otherItem.hideSubItem(gallery_menu_save);
                            if (imageUpdater != null) {
                                otherItem.hideSubItem(set_as_main);
                                otherItem.hideSubItem(edit_avatar);
                                otherItem.hideSubItem(delete_avatar);
                                otherItem.showSubItem(add_photo);
                                otherItem.showSubItem(logout);
//                                otherItem.showSubItem(edit_name);
                            }
                        }
                        if (searchItem != null) {
                            searchItem.setEnabled(!scrolling);
                        }
                        overlaysView.setOverlaysVisible(false, durationFactor);
                        avatarsViewPagerIndicatorView.refreshVisibility(durationFactor);
                        expandAnimator.cancel();
                        avatarImage.getImageReceiver().setAllowStartAnimation(true);
                        avatarImage.getImageReceiver().startAnimation();

                        float value = AndroidUtilities.lerp(expandAnimatorValues, currentExpanAnimatorFracture);
                        expandAnimatorValues[0] = value;
                        expandAnimatorValues[1] = 0f;
                        expandAnimator.setInterpolator(CubicBezierInterpolator.EASE_BOTH);
                        if (!isInLandscapeMode) {
                            expandAnimator.setDuration((long) (value * 250f / durationFactor));
                        } else {
                            expandAnimator.setDuration(0);
                        }
                        topView.setBackgroundColor(getThemedColor(Theme.key_windowBackgroundGray));

                        if (!doNotSetForeground) {
                            BackupImageView imageView = avatarsViewPager.getCurrentItemView();
                            if (imageView != null) {
                                if (imageView.getImageReceiver().getDrawable() instanceof VectorAvatarThumbDrawable) {
                                    avatarImage.drawForeground(false);
                                } else {
                                    avatarImage.drawForeground(true);
                                    avatarImage.setForegroundImageDrawable(imageView.getImageReceiver().getDrawableSafe());
                                }
                            }
                        }
                        expandAnimator.addListener(new AnimatorListenerAdapter() {
                            @Override
                            public void onAnimationEnd(Animator animation) {
                                expandAnimator.removeListener(this);
                                avatarsViewPager.setVisibility(View.GONE);
                                avatarsViewPager.setAlpha(1f);
                            }
                        });
                        avatarImage.setForegroundAlpha(1f);
                        avatarContainer.setVisibility(View.VISIBLE);
                        avatarsViewPager.setAlpha(0f);
                        expandAnimator.start();
                        ignoreScrollOnFullExpand = false;
                        try {
                            avatarContainer.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP, HapticFeedbackConstants.FLAG_IGNORE_VIEW_SETTING);
                        } catch (Exception ignore) {
                        }
                    }

                    avatarContainer.setScaleX(avatarScale);
                    avatarContainer.setScaleY(avatarScale);
                    fixAvatarImageInCenter();

                    if (expandAnimator == null || !expandAnimator.isRunning()) {
                        refreshNameAndOnlineY();
                        nameTextView[1].setTranslationY(nameY);
                        onlineTextView[1].setTranslationX(getOnlineTextViewTranslationXWithOffsets(onlineX));
                        onlineTextView[1].setTranslationY(getOnlineTextViewTranslationYWithOffsets(onlineY));
                        mediaCounterTextView.setTranslationX(onlineX);
                        mediaCounterTextView.setTranslationY(onlineY);
                        updateCollectibleHint();
                    }
                }
            }

            if (openAnimationInProgress && playProfileAnimation == 2) {
                float avX = 0;
                float avY = (actionBar.getOccupyStatusBar() ? AndroidUtilities.statusBarHeight : 0) + ActionBar.getCurrentActionBarHeight() / 2.0f - 21 * AndroidUtilities.density + actionBar.getTranslationY();
//                metaball.setVisibility(View.GONE);

                nameTextView[0].setTranslationX(0);
                nameTextView[0].setTranslationY((float) Math.floor(avY) + AndroidUtilities.dp(1.3f));
                onlineTextView[0].setTranslationX(0);
                onlineTextView[0].setTranslationY((float) Math.floor(avY) + AndroidUtilities.dp(24));
                nameTextView[0].setScaleX(1.0f);
                nameTextView[0].setScaleY(1.0f);

                nameTextView[1].setPivotY(nameTextView[1].getMeasuredHeight());
                nameTextView[1].setScaleX(1.38f);
                nameTextView[1].setScaleY(1.38f);

                avatarScale = lerp(42, 138, avatarAnimationProgress) / 100f;
                pullUpProgress = 0.0f;
                if (storyView != null) {
                    storyView.setExpandProgress(1f);
                }
                if (giftsView != null) {
                    giftsView.setExpandProgress(1f);
                }

                avatarImage.setRoundRadiusForExpand((int) AndroidUtilities.lerp(getSmallAvatarRoundRadius(), 0f, avatarAnimationProgress));
                avatarContainer.setTranslationX(AndroidUtilities.lerp(avX, 0, avatarAnimationProgress));
                avatarContainer.setTranslationY(AndroidUtilities.lerp((float) Math.ceil(avY), 0f, avatarAnimationProgress));
                float extra = (avatarContainer.getMeasuredWidth() - dp(42)) * (avatarScale * 100 / 42);
                timeItem.setTranslationX(avatarContainer.getX() + dp(16) + extra);
                timeItem.setTranslationY(avatarContainer.getY() + dp(15) + extra);
                starBgItem.setTranslationX(avatarContainer.getX() + dp(28) + extra);
                starBgItem.setTranslationY(avatarContainer.getY() + dp(24) + extra);
                starFgItem.setTranslationX(avatarContainer.getX() + dp(28) + extra);
                starFgItem.setTranslationY(avatarContainer.getY() + dp(24) + extra);
                avatarContainer.setScaleX(avatarScale);
                avatarContainer.setScaleY(avatarScale);

                avatarsViewPager.setAlpha(0f);
                avatarsViewPager.setVisibility(View.VISIBLE);
                avatarImage.setBlurRadiusProgressForExpand(avatarAnimationProgress, avatarScale, true);

                overlaysView.setAlphaValue(avatarAnimationProgress, false);
                actionBar.setItemsColor(ColorUtils.blendARGB(peerColor != null ? Color.WHITE : getThemedColor(Theme.key_actionBarDefaultIcon), Color.WHITE, avatarAnimationProgress), false);

                if (scamDrawable != null) {
                    scamDrawable.setColor(ColorUtils.blendARGB(getThemedColor(Theme.key_avatar_subtitleInProfileBlue), Color.argb(179, 255, 255, 255), avatarAnimationProgress));
                }
                if (lockIconDrawable != null) {
                    lockIconDrawable.setColorFilter(peerColor != null ? Color.WHITE : ColorUtils.blendARGB(getThemedColor(Theme.key_chat_lockIcon), Color.WHITE, avatarAnimationProgress), PorterDuff.Mode.MULTIPLY);
                }
                if (verifiedCrossfadeDrawable[1] != null) {
                    verifiedCrossfadeDrawable[1].setProgress(avatarAnimationProgress);
                    nameTextView[1].invalidate();
                }
                if (premiumCrossfadeDrawable[1] != null) {
                    premiumCrossfadeDrawable[1].setProgress(avatarAnimationProgress);
                    nameTextView[1].invalidate();
                }
                updateEmojiStatusDrawableColor(avatarAnimationProgress);

                final FrameLayout.LayoutParams params = (FrameLayout.LayoutParams) avatarContainer.getLayoutParams();
                params.width = (int) AndroidUtilities.lerp(AndroidUtilities.dpf2(100), listView.getMeasuredWidth() / avatarScale, avatarAnimationProgress);
                params.height = (int) AndroidUtilities.lerp(AndroidUtilities.dpf2(100), (extraHeight + newTop) / avatarScale, avatarAnimationProgress);
                params.leftMargin = (int) AndroidUtilities.lerp(AndroidUtilities.dpf2(64f), 0f, avatarAnimationProgress);
                avatarContainer.requestLayout();

                updateCollectibleHint();
            } else if (h <= headerExtraHeight) {
                if (openAnimationInProgress) {
                    avatarScale = lerp(42, 96, diff) / 100f;
                    pullUpProgress = 0.0f;
                } else {
                    float intoSize = 24f;
                    if (avatarGooey != null && avatarGooey.notchInfo != null && avatarGooey.notchInfo.isLikelyCircle) {
                        intoSize = 0.5f * avatarGooey.notchInfo.bounds.width() / AndroidUtilities.density;
                    }
                    avatarScale = lerp(intoSize, 96, diff) / 100f;
                    pullUpProgress = 1.0f - diff;
                }

                if (actionsView != null) {
                    if (openAnimationInProgress) {
                        actionsView.setAlpha(avatarAnimationProgress);
                    } else {
                        actionsView.setAlpha(1f);
                    }
                }

                if (storyView != null) {
                    storyView.invalidate();
                }
                if (giftsView != null) {
                    giftsView.setCollapseProgress(diff, openAnimationInProgress);
                }
                final float nameScale = 1.0f + 0.12f * diff;
                if (expandAnimator == null || !expandAnimator.isRunning()) {
                    avatarContainer.setScaleX(avatarScale);
                    avatarContainer.setScaleY(avatarScale);
                    fixAvatarImageInCenter();
                    avatarContainer.setTranslationY((float) Math.ceil(avatarY));
//                    if (!openAnimationInProgress) {
//                        metaball.setVisibility(View.VISIBLE);
//                        metaball.invalidate();
//                    }
                    final float extra = dp(42) * (avatarScale * 100 / 42) - dp(42);
                    timeItem.setTranslationX(avatarContainer.getX() + dp(16) + extra);
                    timeItem.setTranslationY(avatarContainer.getY() + dp(15) + extra);
                    starBgItem.setTranslationX(avatarContainer.getX() + dp(28) + extra);
                    starBgItem.setTranslationY(avatarContainer.getY() + dp(24) + extra);
                    starFgItem.setTranslationX(avatarContainer.getX() + dp(28) + extra);
                    starFgItem.setTranslationY(avatarContainer.getY() + dp(24) + extra);
                } else {
                    expandAnimator.removeListener(resetListener);
                    expandAnimator.addListener(resetListener);
                }
                final float avatarBottom = (float) Math.floor(endNameY) + (avatarContainer.getHeight() * avatarContainer.getScaleY() + dpf2(8)) * (openAnimationInProgress ? avatarAnimationProgress : diff);
                nameY = avatarBottom + dp(1.3f) + dp(7) * diff + titleAnimationsYDiff * (1f - avatarAnimationProgress);
                onlineY = avatarBottom + dp(24) + (float) Math.floor(11 * AndroidUtilities.density) * diff;
                final float minimizedX = openAnimationInProgress ? 0 : -dpf2(42 + 4);

                if (showStatusButton != null) {
                    showStatusButton.setAlpha((int) (0xFF * diff));
                }

                int viewportWidth = listView.getMeasuredWidth();
                for (int a = 0; a < nameTextView.length; a++) {
                    if (nameTextView[a] == null) {
                        continue;
                    }
                    nameTextView[a].setScaleX(nameScale);
                    nameTextView[a].setScaleY(nameScale);

                    if (a == 1) {
                        textMeasured = true;
                        needLayoutText(diff, 0, false);
                    }

                    FrameLayout.LayoutParams params1 = (FrameLayout.LayoutParams) nameTextView[a].getLayoutParams();
                    float nameX = viewportWidth / 2f - (params1.leftMargin + Math.min(nameTextView[a].getExactWidth(), a == 1 ? params1.width : viewportWidth) * nameScale * 0.5f);
                    FrameLayout.LayoutParams params2 = (FrameLayout.LayoutParams) onlineTextView[a].getLayoutParams();
                    float onlineX = viewportWidth / 2f - (params2.leftMargin + Math.min(onlineTextView[hasFallbackPhoto ? 3 : a].getExactWidth(), a == 1 ? params2.width : viewportWidth) * 0.5f);

                    if (a == 1) {
                        this.nameX = nameX;
                        this.onlineX = onlineX;
                    }
                    nameX = AndroidUtilities.lerp(minimizedX, nameX, diff);
                    onlineX = AndroidUtilities.lerp(minimizedX, onlineX, diff);

                    if (expandAnimator == null || !expandAnimator.isRunning()) {
                        nameTextView[a].setTranslationX(nameX);
                        nameTextView[a].setTranslationY(nameY);
                        onlineTextView[a].setTranslationX(getOnlineTextViewTranslationXWithOffsets(onlineX));
                        onlineTextView[a].setTranslationY(getOnlineTextViewTranslationYWithOffsets(onlineY));
                        if (a == 1) {
                            mediaCounterTextView.setTranslationX(onlineX);
                        }
                    }
                    if (a == 1) updateTextLayoutBasedOnTranslation();
                }
                mediaCounterTextView.setTranslationY(onlineY);
                updateCollectibleHint();
            }

            if (!textMeasured && (expandAnimator == null || !expandAnimator.isRunning())) {
                needLayoutText(diff);
            }
        }

        updateExtraViews(newTop);
        if (diff >= 1f) {
            avatarImage.setAlpha(1f);
        }
    }

    private void updateExtraViews(float newTop) {
        if (isPulledDown || (overlaysView != null && overlaysView.animator != null && overlaysView.animator.isRunning())) {
            final ViewGroup.LayoutParams overlaysLp = overlaysView.getLayoutParams();
            overlaysLp.width = listView.getMeasuredWidth();
            overlaysLp.height = (int) (extraHeight + newTop);
            overlaysView.requestLayout();
        }
        if (topView != null) {
            topView.backgroundGradientMatrix.setTranslate(0f, avatarY - topView.backgroundGradientY);
            if (topView.backgroundGradient != null) {
                topView.backgroundGradient.setLocalMatrix(topView.backgroundGradientMatrix);
            }
            topView.invalidate();
        }
        updateEmojiStatusEffectPosition();
        updateActionsPosition();
        updateMusicPosition();
    }

    public void calculatePositionsOnFirstLoad() {
        float endNameY =
            (actionBar.getOccupyStatusBar() ? AndroidUtilities.statusBarHeight : 0)
                + ActionBar.getCurrentActionBarHeight()
                - dp(21)
                + actionBar.getTranslationY();

        avatarY = endNameY;
        avatarScale = 138 / 100f;
        pullUpProgress = 0.0f;
        fixAvatarImageInCenter();

        refreshNameAndOnlineY();

        needLayoutText(1f);

        float nameScale = 1.0f + 0.12f;
        for (int a = 0; a < nameTextView.length; a++) {
            if (nameTextView[a] == null) {
                continue;
            }
            FrameLayout.LayoutParams params = (FrameLayout.LayoutParams) nameTextView[a].getLayoutParams();
            float nameX = listView.getMeasuredWidth() / 2f - (params.leftMargin + nameTextView[a].getExactWidth() * nameScale * 0.5f);
            params = (FrameLayout.LayoutParams) onlineTextView[a].getLayoutParams();
            float onlineX = listView.getMeasuredWidth() / 2f - (params.leftMargin + onlineTextView[a].getExactWidth() * 0.5f);

            if (a == 1) {
                this.nameX = nameX;
                this.onlineX = onlineX;
            }
        }

        if (playProfileAnimation != 2)
            storyView.setAlpha(1f);
        avatarContainer.setAlpha(1f);
        avatarImage.setAlpha(1f);
    }

    private void setForegroundImage(boolean secondParent) {
        Drawable drawable = avatarImage.getImageReceiver().getDrawable();
        if (drawable instanceof VectorAvatarThumbDrawable) {
            avatarImage.setForegroundImage(null, null, drawable);
        } else if (drawable instanceof AnimatedFileDrawable) {
            AnimatedFileDrawable fileDrawable = (AnimatedFileDrawable) drawable;
            avatarImage.setForegroundImage(null, null, fileDrawable);
            if (secondParent) {
                fileDrawable.addSecondParentView(avatarImage);
            }
        } else {
            ImageLocation location = avatarsViewPager.getImageLocation(0);
            String filter;
            if (location != null && location.imageType == FileLoader.IMAGE_TYPE_ANIMATION) {
                filter = "avatar";
            } else {
                filter = null;
            }
            avatarImage.setForegroundImage(location, filter, drawable);
        }
    }

    float expandNameYStartedFrom;
    float expandOnlineYStartedFrom;

    private void refreshNameAndOnlineY() {
        if (isPulledDown && expandAnimator != null && expandAnimator.isRunning()) {
            nameY = expandNameYStartedFrom;
            onlineY = expandOnlineYStartedFrom;
            return;
        }
        final int newTop = ActionBar.getCurrentActionBarHeight() + (actionBar.getOccupyStatusBar() ? AndroidUtilities.statusBarHeight : 0);
        refreshNameAndOnlineYBasedOnExpand(
                calculateHeaderExtraDiff(),
                newTop
        );
    }

    private void refreshNameAndOnlineYBasedOnExpand(float diff, float newTop) {
        float scale;
        float expand = Math.max(0f, Math.min(1f, (extraHeight - getHeaderExtraHeight()) / (listView.getMeasuredWidth() - newTop - getHeaderOnlyExtraHeight())));
        if (extraHeight < getHeaderExtraHeight() && expand < 0.33f) {
            scale = (24 + (54f + (42 - 24)) * diff) / 42.0f;
        } else {
            scale = AndroidUtilities.lerp((42f + 54f) / 42f, (42f + 42f + 54f) / 42f, Math.min(1f, expand * 3f));
        }

        float endNameY = (actionBar.getOccupyStatusBar() ? AndroidUtilities.statusBarHeight : 0)
                + ActionBar.getCurrentActionBarHeight() / 2f * (1f + diff)
                - 21 * AndroidUtilities.density
                + actionBar.getTranslationY();

        float avatarBottom = (float) Math.floor(endNameY) + (AndroidUtilities.dp(42) * scale + dpf2(8)) * diff;
        nameY = avatarBottom + AndroidUtilities.dp(1.3f) + AndroidUtilities.dp(7) * diff;
        onlineY = avatarBottom + AndroidUtilities.dp(24) + (float) Math.floor(11 * AndroidUtilities.density) * diff;
    }

    public RecyclerListView getListView() {
        return listView;
    }

    private void needLayoutText(float diff) {
        needLayoutText(diff, 0, true);
    }

    private void updateTextLayoutBasedOnTranslation() {
        FrameLayout.LayoutParams params = (FrameLayout.LayoutParams) nameTextView[1].getLayoutParams();
        int old = params.width;
        int viewWidth = AndroidUtilities.isTablet() ? AndroidUtilities.dp(490) : AndroidUtilities.displaySize.x;
        float x = params.leftMargin + nameTextView[1].getTranslationX();
        params.width = Math.min(params.width,(int) ((viewWidth - AndroidUtilities.dp(18f)) / nameTextView[1].getScaleX() - x));
        if (params.width != old) {
            nameTextView[1].requestLayout();
        }
    }

    private void needLayoutText(float diff, int startWidth, boolean invalidate) {
        FrameLayout.LayoutParams layoutParams;
        float minScale, maxScale;
        if (extraHeight > getHeaderExtraHeight() || expandAnimator != null && expandAnimator.isRunning()) {
            minScale = maxScale = nameTextView[1].getScaleX();
        } else if (diff >= 1f){
            minScale = maxScale = Math.max(nameTextView[1].getScaleX(), 1.12f);
        } else {
            minScale = 1f;
            maxScale = 1.12f;
        }

        int viewWidth = AndroidUtilities.isTablet() ? AndroidUtilities.dp(490) : AndroidUtilities.displaySize.x;
        int extra = 0;
        if (editItemVisible) {
            extra += 48;
        }
        if (callItemVisible) {
            extra += 48;
        }
        if (videoCallItemVisible) {
            extra += 48;
        }
        if (searchItem != null) {
            extra += 48;
        }
        int buttonsWidth = AndroidUtilities.dp((openAnimationInProgress ? 118 : 56) + 8 + (40 + extra * (1.0f - mediaHeaderAnimationProgress)));
        int minOnlineWidth = viewWidth - buttonsWidth;
        int minWidth = (int) (minOnlineWidth / (startWidth != 0 ? 1f : minScale));
        int maxOnlineWidth = startWidth != 0 ? startWidth : viewWidth - AndroidUtilities.dp(18) * 2;
        int maxWidth = startWidth != 0 ? startWidth : (int) (maxOnlineWidth / maxScale);

        int width = AndroidUtilities.lerp(minWidth, maxWidth, CubicBezierInterpolator.EASE_IN.getInterpolation(diff));
        layoutParams = (FrameLayout.LayoutParams) nameTextView[1].getLayoutParams();
        int prevWidth = layoutParams.width;
        layoutParams.width = (int) Math.ceil(width);

        if (invalidate) {
            float x = layoutParams.leftMargin + nameTextView[1].getTranslationX();
            layoutParams.width = Math.min(layoutParams.width, (int) ((viewWidth - AndroidUtilities.dp(18f)) / nameTextView[1].getScaleX() - x));
        }
        if (invalidate && layoutParams.width != prevWidth) {
            nameTextView[1].requestLayout();
        }

        width = AndroidUtilities.lerp(minOnlineWidth, maxOnlineWidth, CubicBezierInterpolator.EASE_IN.getInterpolation(diff));
        layoutParams = (FrameLayout.LayoutParams) onlineTextView[1].getLayoutParams();
        FrameLayout.LayoutParams layoutParams2 = (FrameLayout.LayoutParams) mediaCounterTextView.getLayoutParams();
        prevWidth = layoutParams.width;
        layoutParams2.width = layoutParams.width = (int) Math.ceil(width);

        if (prevWidth != layoutParams.width) {
            onlineTextView[2].getLayoutParams().width = layoutParams.width;
            onlineTextView[2].requestLayout();
            onlineTextView[3].getLayoutParams().width = layoutParams.width;
            onlineTextView[3].requestLayout();
            onlineTextView[1].requestLayout();
            mediaCounterTextView.requestLayout();
        }
    }

    private void fixLayout() {
        if (fragmentView == null) {
            return;
        }
        fragmentView.getViewTreeObserver().addOnPreDrawListener(new ViewTreeObserver.OnPreDrawListener() {
            @Override
            public boolean onPreDraw() {
                if (fragmentView != null) {
                    checkListViewScroll();
                    needLayout(true);
                    fragmentView.getViewTreeObserver().removeOnPreDrawListener(this);
                }
                return true;
            }
        });
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        if (sharedMediaLayout != null) {
            sharedMediaLayout.onConfigurationChanged(newConfig);
        }
        invalidateIsInLandscapeMode();
        if (isInLandscapeMode && actionsView != null) {
            actionsView.drawingBlur(false);
        }
        if (isInLandscapeMode && musicView != null) {
            musicView.drawingBlur(false);
        }
        if (isInLandscapeMode && isPulledDown) {
            final View view = layoutManager.findViewByPosition(0);
            if (view != null) {
                listView.scrollBy(0, view.getTop() - getHeaderExtraHeight());
            }
        }
        fixLayout();
    }

    private void invalidateIsInLandscapeMode() {
        final Point size = new Point();
        final Display display = getParentActivity().getWindowManager().getDefaultDisplay();
        display.getSize(size);
        isInLandscapeMode = size.x > size.y;
    }

    @SuppressWarnings("unchecked")
    @Override
    public void didReceivedNotification(int id, int account, final Object... args) {
        if (id == NotificationCenter.uploadStoryEnd || id == NotificationCenter.chatWasBoostedByUser) {
            checkCanSendStoryForPosting();
        } else if (id == NotificationCenter.updateInterfaces) {
            int mask = (Integer) args[0];
            boolean infoChanged = (mask & MessagesController.UPDATE_MASK_AVATAR) != 0 || (mask & MessagesController.UPDATE_MASK_NAME) != 0 || (mask & MessagesController.UPDATE_MASK_STATUS) != 0 || (mask & MessagesController.UPDATE_MASK_EMOJI_STATUS) != 0;
            if (userId != 0) {
                if (infoChanged) {
                    updateProfileData(true);
                }
                if ((mask & MessagesController.UPDATE_MASK_PHONE) != 0) {
                    if (listView != null) {
                        RecyclerListView.Holder holder = (RecyclerListView.Holder) listView.findViewHolderForPosition(phoneRow);
                        if (holder != null) {
                            listAdapter.onBindViewHolder(holder, phoneRow);
                        }
                    }
                }
            } else if (chatId != 0) {
                if ((mask & MessagesController.UPDATE_MASK_CHAT) != 0 || (mask & MessagesController.UPDATE_MASK_CHAT_AVATAR) != 0 || (mask & MessagesController.UPDATE_MASK_CHAT_NAME) != 0 || (mask & MessagesController.UPDATE_MASK_CHAT_MEMBERS) != 0 || (mask & MessagesController.UPDATE_MASK_STATUS) != 0 || (mask & MessagesController.UPDATE_MASK_EMOJI_STATUS) != 0) {
                    if ((mask & MessagesController.UPDATE_MASK_CHAT) != 0) {
                        updateListAnimated(true);
                    } else {
                        updateOnlineCount(true);
                    }
                    updateProfileData(true);
                }
                if (infoChanged) {
                    if (listView != null) {
                        int count = listView.getChildCount();
                        for (int a = 0; a < count; a++) {
                            View child = listView.getChildAt(a);
                            if (child instanceof UserCell) {
                                ((UserCell) child).update(mask);
                            }
                        }
                    }
                }
            }
        } else if (id == NotificationCenter.chatOnlineCountDidLoad) {
            Long chatId = (Long) args[0];
            if (chatInfo == null || currentChat == null || currentChat.id != chatId) {
                return;
            }
            chatInfo.online_count = (Integer) args[1];
            updateOnlineCount(true);
            updateProfileData(false);
        } else if (id == NotificationCenter.contactsDidLoad || id == NotificationCenter.channelRightsUpdated) {
            createActionBarMenu(true);
        } else if (id == NotificationCenter.encryptedChatCreated) {
            if (creatingChat) {
                AndroidUtilities.runOnUIThread(() -> {
                    getNotificationCenter().removeObserver(ProfileActivity.this, NotificationCenter.closeChats);
                    getNotificationCenter().postNotificationName(NotificationCenter.closeChats);
                    TLRPC.EncryptedChat encryptedChat = (TLRPC.EncryptedChat) args[0];
                    Bundle args2 = new Bundle();
                    args2.putInt("enc_id", encryptedChat.id);
                    presentFragment(new ChatActivity(args2), true);
                });
            }
        } else if (id == NotificationCenter.encryptedChatUpdated) {
            TLRPC.EncryptedChat chat = (TLRPC.EncryptedChat) args[0];
            if (currentEncryptedChat != null && chat.id == currentEncryptedChat.id) {
                currentEncryptedChat = chat;
                updateListAnimated(false);
                if (flagSecure != null) {
                    flagSecure.invalidate();
                }
            }
        } else if (id == NotificationCenter.blockedUsersDidLoad) {
            boolean oldValue = userBlocked;
            userBlocked = getMessagesController().blockePeers.indexOfKey(userId) >= 0;
            if (oldValue != userBlocked) {
                createActionBarMenu(true);
                updateListAnimated(false);
            }
        } else if (id == NotificationCenter.groupCallUpdated) {
            Long chatId = (Long) args[0];
            if (currentChat != null && chatId == currentChat.id && ChatObject.canManageCalls(currentChat)) {
                TLRPC.ChatFull chatFull = MessagesController.getInstance(currentAccount).getChatFull(chatId);
                if (chatFull != null) {
                    if (chatInfo != null) {
                        chatFull.participants = chatInfo.participants;
                    }
                    chatInfo = chatFull;
                }
                if (sharedMediaLayout != null) {
                    sharedMediaLayout.setChatInfo(chatInfo);
                }
                if (chatInfo != null && (chatInfo.call == null && !hasVoiceChatItem || chatInfo.call != null && hasVoiceChatItem)) {
                    createActionBarMenu(false);
                }
                if (storyView != null && chatInfo != null) {
                    storyView.setStories(chatInfo.stories);
                }
                if (giftsView != null) {
                    giftsView.update();
                }
                if (avatarImage != null) {
                    avatarImage.setHasStories(needInsetForStories());
                }
                if (chatId != 0) {
                    boolean gift = !BuildVars.IS_BILLING_UNAVAILABLE && !getMessagesController().premiumPurchaseBlocked() && chatInfo != null && chatInfo.stargifts_available;
                    otherItem.setSubItemShown(gift_premium, gift);
                    if (actionsView != null) {
                        actionsView.set(ProfileActionsView.KEY_GIFT, gift);
                    }
                }
            }
        } else if (id == NotificationCenter.chatInfoDidLoad) {
            final TLRPC.ChatFull chatFull = (TLRPC.ChatFull) args[0];
            if (chatFull.id == chatId) {
                final boolean byChannelUsers = (Boolean) args[2];
                if (chatInfo instanceof TLRPC.TL_channelFull) {
                    if (chatFull.participants == null) {
                        chatFull.participants = chatInfo.participants;
                    }
                }
                final boolean loadChannelParticipants = chatInfo == null && chatFull instanceof TLRPC.TL_channelFull;
                chatInfo = chatFull;
                if (mergeDialogId == 0 && chatInfo.migrated_from_chat_id != 0) {
                    mergeDialogId = -chatInfo.migrated_from_chat_id;
                    getMediaDataController().getMediaCount(mergeDialogId, topicId, MediaDataController.MEDIA_PHOTOVIDEO, classGuid, true);
                }
                fetchUsersFromChannelInfo();
                if (avatarsViewPager != null && !isTopic) {
                    avatarsViewPager.setChatInfo(chatInfo);
                }
                updateListAnimated(true);
                TLRPC.Chat newChat = getMessagesController().getChat(chatId);
                if (newChat != null) {
                    currentChat = newChat;
                    createActionBarMenu(true);
                }
                if (flagSecure != null) {
                    flagSecure.invalidate();
                }
                if (currentChat.megagroup && (loadChannelParticipants || !byChannelUsers)) {
                    getChannelParticipants(true);
                }

                updateAutoDeleteItem();
                updateTtlIcon();
                if (storyView != null && chatInfo != null) {
                    storyView.setStories(chatInfo.stories);
                }
                if (giftsView != null) {
                    giftsView.update();
                }
                if (avatarImage != null) {
                    avatarImage.setHasStories(needInsetForStories());
                }
                if (sharedMediaLayout != null) {
                    sharedMediaLayout.setChatInfo(chatInfo);
                }
            }
        } else if (id == NotificationCenter.closeChats) {
            removeSelfFromStack(true);
        } else if (id == NotificationCenter.closeProfileActivity) {
            final long dialogId = (long) args[0];
            final boolean includingLast = (boolean) args[1];
            if (dialogId == getDialogId() && (!includingLast ? parentLayout.getLastFragment() != this : true)) {
                if (parentLayout.getLastFragment() == this) {
                    finishFragment();
                } else {
                    removeSelfFromStack(true);
                }
            }
        } else if (id == NotificationCenter.botInfoDidLoad) {
            final TL_bots.BotInfo info = (TL_bots.BotInfo) args[0];
            if (info.user_id == userId) {
                botInfo = info;
                updateListAnimated(false);
            }
        } else if (id == NotificationCenter.userInfoDidLoad) {
            final long uid = (Long) args[0];
            if (uid == userId) {
                userInfo = (TLRPC.UserFull) args[1];
                if (ratingView != null) {
                    ratingView.set(userInfo.stars_rating);
                }
                if (storyView != null) {
                    storyView.setStories(userInfo.stories);
                }
                if (giftsView != null) {
                    giftsView.update();
                }
                if (avatarImage != null) {
                    avatarImage.setHasStories(needInsetForStories());
                }
                if (sharedMediaLayout != null) {
                    sharedMediaLayout.setUserInfo(userInfo);
                }
                if (imageUpdater != null) {
                    if (listAdapter != null && !TextUtils.equals(userInfo.about, currentBio)) {
                        listAdapter.notifyItemChanged(bioRow);
                    }
                } else {
                    if (!openAnimationInProgress && !isCallAvailable) {
                        createActionBarMenu(true);
                    } else {
                        recreateMenuAfterAnimation = true;
                    }
                    updateListAnimated(false);
                    if (sharedMediaLayout != null) {
                        sharedMediaLayout.setCommonGroupsCount(userInfo.common_chats_count);
                        updateSelectedMediaTabText();
                        if (sharedMediaPreloader == null || sharedMediaPreloader.isMediaWasLoaded()) {
                            resumeDelayedFragmentAnimation();
                            needLayout(true);
                        }
                    }
                }
                updateAutoDeleteItem();
                updateTtlIcon();
                if (profileChannelMessageFetcher == null && !isSettings()) {
                    profileChannelMessageFetcher = new ProfileChannelCell.ChannelMessageFetcher(currentAccount);
                    profileChannelMessageFetcher.subscribe(() -> updateListAnimated(false));
                    profileChannelMessageFetcher.fetch(userInfo);
                }
                if (!isSettings()) {
                    ProfileBirthdayEffect.BirthdayEffectFetcher oldFetcher = birthdayFetcher;
                    birthdayFetcher = ProfileBirthdayEffect.BirthdayEffectFetcher.of(currentAccount, userInfo, birthdayFetcher);
                    createdBirthdayFetcher = birthdayFetcher != oldFetcher;
                    if (birthdayFetcher != null) {
                        birthdayFetcher.subscribe(this::createBirthdayEffect);
                    }
                }
                if (otherItem != null) {
                    if (hasPrivacyCommand()) {
                        otherItem.showSubItem(bot_privacy);
                    } else {
                        otherItem.hideSubItem(bot_privacy);
                    }

                    final boolean forwardsAllowed = !userInfo.noforwards_my_enabled && !userInfo.noforwards_peer_enabled;
                    otherItem.setSubItemShown(enable_no_forwards, forwardsAllowed);
                    otherItem.setSubItemShown(disable_no_forwards, !forwardsAllowed);
                }
            }
        } else if (id == NotificationCenter.privacyRulesUpdated) {

        } else if (id == NotificationCenter.didReceiveNewMessages) {
            final boolean scheduled = (Boolean) args[2];
            if (scheduled) {
                return;
            }
            final long did = getDialogId();
            if (did == (Long) args[0]) {
                boolean enc = DialogObject.isEncryptedDialog(did);
                ArrayList<MessageObject> arr = (ArrayList<MessageObject>) args[1];
                for (int a = 0; a < arr.size(); a++) {
                    MessageObject obj = arr.get(a);
                    if (currentEncryptedChat != null && obj.messageOwner.action instanceof TLRPC.TL_messageEncryptedAction && obj.messageOwner.action.encryptedAction instanceof TLRPC.TL_decryptedMessageActionSetMessageTTL) {
                        TLRPC.TL_decryptedMessageActionSetMessageTTL action = (TLRPC.TL_decryptedMessageActionSetMessageTTL) obj.messageOwner.action.encryptedAction;
                        if (listAdapter != null) {
                            listAdapter.notifyDataSetChanged();
                        }
                    }
                }
            }
        } else if (id == NotificationCenter.emojiLoaded) {
            if (listView != null) {
                listView.invalidateViews();
            }
        } else if (id == NotificationCenter.reloadInterface) {
            updateListAnimated(false);
        } else if (id == NotificationCenter.newSuggestionsAvailable) {
            final int prevRow1 = passwordSuggestionRow;
            final int prevRow2 = phoneSuggestionRow;
            final int prevRow3 = graceSuggestionRow;
            updateRowsIds();
            if (listAdapter != null && (prevRow1 != passwordSuggestionRow || prevRow2 != phoneSuggestionRow || prevRow3 != graceSuggestionRow)) {
                listAdapter.notifyDataSetChanged();
            }
        } else if (id == NotificationCenter.topicsDidLoaded) {
            if (isTopic) {
                updateProfileData(false);
            }
        } else if (id == NotificationCenter.updateSearchSettings) {
            if (searchAdapter != null) {
                searchAdapter.searchArray = searchAdapter.onCreateSearchArray(ProfileActivity.this);
                searchAdapter.recentSearches.clear();
                searchAdapter.updateSearchArray();
                searchAdapter.search(searchAdapter.lastSearchString);
            }
        } else if (id == NotificationCenter.reloadDialogPhotos) {
            updateProfileData(false);
        } else if (id == NotificationCenter.storiesUpdated || id == NotificationCenter.storiesReadUpdated) {
            if (avatarImage != null) {
                avatarImage.setHasStories(needInsetForStories());
                updateAvatarRoundRadius();
            }
            if (storyView != null) {
                if (userInfo != null) {
                    storyView.setStories(userInfo.stories);
                } else if (chatInfo != null) {
                    storyView.setStories(chatInfo.stories);
                }
            }
        } else if (id == NotificationCenter.userIsPremiumBlockedUpadted) {
            if (otherItem != null) {
                otherItem.setSubItemShown(start_secret_chat, DialogObject.isEmpty(getMessagesController().isUserContactBlocked(userId)));
            }
            updateEditColorIcon();
        } else if (id == NotificationCenter.currentUserPremiumStatusChanged) {
            updateEditColorIcon();
        } else if (id == NotificationCenter.starBalanceUpdated) {
            updateListAnimated(false);
        } else if (id == NotificationCenter.botStarsUpdated) {
            updateListAnimated(false);
        } else if (id == NotificationCenter.botStarsTransactionsLoaded) {
            updateListAnimated(false);
        } else if (id == NotificationCenter.dialogDeleted) {
            final long dialogId = (long) args[0];
            if (getDialogId() == dialogId) {
                if (parentLayout != null && parentLayout.getLastFragment() == this) {
                    finishFragment();
                } else {
                    removeSelfFromStack();
                }
            }
        } else if (id == NotificationCenter.channelRecommendationsLoaded) {
            final long dialogId = (long) args[0];
            if (sharedMediaRow < 0 && dialogId == getDialogId()) {
                updateRowsIds();
                updateSelectedMediaTabText();
                if (listAdapter != null) {
                    listAdapter.notifyDataSetChanged();
                }
            }
        } else if (id == NotificationCenter.starUserGiftsLoaded) {
            final long dialogId = (long) args[0];
            if (dialogId == getDialogId() && !isSettings()) {
                if (sharedMediaRow < 0) {
                    updateRowsIds();
                    updateSelectedMediaTabText();
                    if (listAdapter != null) {
                        listAdapter.notifyDataSetChanged();
                    }
                    AndroidUtilities.runOnUIThread(() -> {
                        if (sharedMediaLayout != null) {
                            sharedMediaLayout.updateTabs(true);
                            sharedMediaLayout.updateAdapters();
                        }
                    });
                } else if (sharedMediaLayout != null) {
                    sharedMediaLayout.updateTabs(true);
                }
            }
        } else if (id == NotificationCenter.profileMusicUpdated) {
            if ((long) args[0] == getDialogId() && userId > 0) {
                final TLRPC.UserFull newUserInfo = getMessagesController().getUserFull(userId);
                if (newUserInfo != null) {
                    userInfo = newUserInfo;
                }
                updateRowsIds();
                updateSelectedMediaTabText();
                if (listView != null && listView.isComputingLayout()) {
                    listView.post(() -> {
                        if (listView.isComputingLayout()) {
                            return;
                        }
                        if (listAdapter != null) {
                            listAdapter.notifyDataSetChanged();
                        }
                    });
                } else if (listAdapter != null) {
                    listAdapter.notifyDataSetChanged();
                }
            }
        } else if (id == NotificationCenter.updatedChatRanks) {
            final long chatId = (long) args[0];
            final long userId = (long) args[1];
            if (currentChat == null || currentChat.id != chatId) return;
            final String rank = (String) args[2];

            if (participantsMap != null) {
                final TLRPC.ChatParticipant participant = participantsMap.get(userId);
                if (participant != null) {
                    participant.setRank(userId, rank);
                }
            }
            if (currentChannelParticipant != null && currentChannelParticipant.user_id == userId) {
                currentChannelParticipant.rank = rank;
            }
            for (int i = 0; i < visibleChatParticipants.size(); ++i) {
                final TLRPC.ChatParticipant p = visibleChatParticipants.get(i);
                p.setRank(userId, rank);
            }
            AndroidUtilities.updateVisibleRows(listView);
        }
    }

    private void updateAutoDeleteItem() {
        if (autoDeleteItem == null || autoDeletePopupWrapper == null) {
            return;
        }
        int ttl = 0;
        if (userInfo != null || chatInfo != null) {
            ttl = userInfo != null ? userInfo.ttl_period : chatInfo.ttl_period;
        }
        autoDeleteItemDrawable.setTime(ttl);
        autoDeletePopupWrapper.updateItems(ttl);
    }

    private void updateTimeItem() {
        if (timerDrawable == null) {
            return;
        }
        final boolean fromMonoforum = previousTransitionFragment instanceof ChatActivity && ChatObject.isMonoForum(((ChatActivity) previousTransitionFragment).getCurrentChat());
        if (fromMonoforum) {
            timeItem.setTag(null);
            timeItem.setVisibility(View.GONE);
        } else if (currentEncryptedChat != null) {
            timerDrawable.setTime(currentEncryptedChat.ttl);
            timeItem.setTag(1);
            timeItem.setVisibility(View.VISIBLE);
        } else if (userInfo != null) {
            timerDrawable.setTime(userInfo.ttl_period);
            if (needTimerImage && userInfo.ttl_period != 0) {
                timeItem.setTag(1);
                timeItem.setVisibility(View.VISIBLE);
            } else {
                timeItem.setTag(null);
                timeItem.setVisibility(View.GONE);
            }
        } else if (chatInfo != null) {
            timerDrawable.setTime(chatInfo.ttl_period);
            if (needTimerImage && chatInfo.ttl_period != 0) {
                timeItem.setTag(1);
                timeItem.setVisibility(View.VISIBLE);
            } else {
                timeItem.setTag(null);
                timeItem.setVisibility(View.GONE);
            }
        } else {
            timeItem.setTag(null);
            timeItem.setVisibility(View.GONE);
        }
    }

    private void updateStar() {
        if (starBgItem == null || starFgItem == null) return;
        if (needStarImage && currentChat != null && (currentChat.flags2 & 2048) != 0) {
            starFgItem.setTag(1);
            starFgItem.setVisibility(View.VISIBLE);
            starBgItem.setTag(1);
            starBgItem.setVisibility(View.VISIBLE);
        } else {
            starFgItem.setTag(null);
            starFgItem.setVisibility(View.GONE);
            starBgItem.setTag(null);
            starBgItem.setVisibility(View.GONE);
        }
    }

    @Override
    public boolean needDelayOpenAnimation() {
        if (playProfileAnimation == 0) {
            return true;
        }
        return false;
    }

    @Override
    public void mediaCountUpdated() {
        if (sharedMediaLayout != null && sharedMediaPreloader != null) {
            sharedMediaLayout.setNewMediaCounts(sharedMediaPreloader.getLastMediaCount());
        }
        updateSharedMediaRows();
        updateSelectedMediaTabText();

        if (userInfo != null) {
            resumeDelayedFragmentAnimation();
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        if (sharedMediaLayout != null) {
            sharedMediaLayout.onResume();
        }
        invalidateIsInLandscapeMode();
        if (listAdapter != null) {
            // saveScrollPosition();
            firstLayout = true;
            listAdapter.notifyDataSetChanged();
        }
        if (!parentLayout.isInPreviewMode() && blurredView != null && blurredView.getVisibility() == View.VISIBLE) {
            blurredView.setVisibility(View.GONE);
            blurredView.setBackground(null);
        }

        if (imageUpdater != null) {
            imageUpdater.onResume();
            setParentActivityTitle(LocaleController.getString(R.string.Settings));
        }

        updateProfileData(true);
        fixLayout();
        if (nameTextView[1] != null) {
            setParentActivityTitle(nameTextView[1].getText());
        }
        if (userId != 0) {
            final TLRPC.User user = getMessagesController().getUser(userId);
            if (user != null && user.photo == null) {
                if (extraHeight >= getHeaderExtraHeight()) {
                    expandAnimator.cancel();
                    expandAnimatorValues[0] = 1f;
                    expandAnimatorValues[1] = 0f;
                    setAvatarExpandProgress(1f);
                    avatarsViewPager.setVisibility(View.GONE);
                    extraHeight = getHeaderExtraHeight();
                    allowPullingDown = false;
                    layoutManager.scrollToPositionWithOffset(0, getHeaderExtraHeight() - listView.getPaddingTop());
                }
            }
        }
        if (flagSecure != null) {
            flagSecure.attach();
        }
        updateItemsUsername();
        needLayout(false);
    }

    @Override
    public void onPause() {
        super.onPause();
        if (undoView != null) {
            undoView.hide(true, 0);
        }
        if (imageUpdater != null) {
            imageUpdater.onPause();
        }
        if (flagSecure != null) {
            flagSecure.detach();
        }
        if (sharedMediaLayout != null) {
            sharedMediaLayout.onPause();
        }
    }


    @Override
    public boolean canParentTabsSlide(MotionEvent ev, boolean forward) {
        return isSwipeBackEnabled(ev);
    }

    @Override
    public boolean isSwipeBackEnabled(MotionEvent event) {
        if (avatarsViewPager != null && avatarsViewPager.getVisibility() == View.VISIBLE && avatarsViewPager.getRealCount() > 1) {
            avatarsViewPager.getHitRect(rect);
            if (event != null && rect.contains((int) event.getX(), (int) event.getY() - actionBar.getMeasuredHeight())) {
                return false;
            }
        }
        if (sharedMediaRow == -1 || sharedMediaLayout == null) {
            return true;
        }
        if (!sharedMediaLayout.isSwipeBackEnabled()) {
            return false;
        }
        sharedMediaLayout.getHitRect(rect);
        if (event != null && !rect.contains((int) event.getX(), (int) event.getY() - actionBar.getMeasuredHeight())) {
            return true;
        }
        return sharedMediaLayout.isCurrentTabFirst();
    }

    @Override
    public boolean canBeginSlide() {
        if (!sharedMediaLayout.isSwipeBackEnabled()) {
            return false;
        }
        return super.canBeginSlide();
    }

    public UndoView getUndoView() {
        return undoView;
    }

    public boolean onBackPressed() {
        if (closeSheet()) {
            return false;
        }
        if (sharedMediaLayout != null && sharedMediaLayout.scrollSlidingTextTabStrip != null && sharedMediaLayout.scrollSlidingTextTabStrip.isReordering()) {
            stopTabsReorder();
            return false;
        }
        return actionBar.isEnabled() && (sharedMediaRow == -1 || sharedMediaLayout == null || !sharedMediaLayout.closeActionMode());
    }

    public boolean isSettings() {
        return imageUpdater != null && !myProfile;
    }

    @Override
    public void onBecomeFullyHidden() {
        if (undoView != null) {
            undoView.hide(true, 0);
        }
        super.onBecomeFullyHidden();
        fullyVisible = false;
    }

    public void setPlayProfileAnimation(int type) {
        SharedPreferences preferences = MessagesController.getGlobalMainSettings();
        if (!AndroidUtilities.isTablet()) {
            needTimerImage = type != 0;
            needStarImage = type != 0;
            updateStar();
            if (preferences.getBoolean("view_animations", true)) {
                playProfileAnimation = type;
            } else if (type == 2) {
                expandPhoto = true;
            }
        }
    }

    private void updateSharedMediaRows() {
        if (listAdapter == null) {
            return;
        }
        updateListAnimated(false);
    }

    public boolean isFragmentOpened;

    @Override
    public void onTransitionAnimationStart(boolean isOpen, boolean backward) {
        super.onTransitionAnimationStart(isOpen, backward);
        isFragmentOpened = isOpen;

        if (isOpen && actionsView != null) {
            actionsView.startAnimatedActions();
        }

        if (!isOpen && callToActionItem != null && callToActionItem.getTag() != null) {
            if (callToActionItem.getTag() instanceof ActionBarMenuItem) {
                ((ActionBarMenuItem) callToActionItem.getTag()).setAlpha(1f);
            }
        }

        if ((!isOpen && backward || isOpen && !backward) && playProfileAnimation != 0 && allowProfileAnimation && !isPulledDown) {
            openAnimationInProgress = true;
            if (!isOpen) {
                captureBackwardInitialValues();
                if (collectibleHint != null) {
                    collectibleHint.hide(true);
                }
            }
        }
        if (isOpen) {
            if (imageUpdater != null) {
                transitionIndex = getNotificationCenter().setAnimationInProgress(transitionIndex, new int[]{NotificationCenter.dialogsNeedReload, NotificationCenter.closeChats, NotificationCenter.mediaCountDidLoad, NotificationCenter.mediaCountsDidLoad, NotificationCenter.userInfoDidLoad, NotificationCenter.needCheckSystemBarColors});
            } else {
                transitionIndex = getNotificationCenter().setAnimationInProgress(transitionIndex, new int[]{NotificationCenter.dialogsNeedReload, NotificationCenter.closeChats, NotificationCenter.mediaCountDidLoad, NotificationCenter.mediaCountsDidLoad, NotificationCenter.needCheckSystemBarColors});
            }
            if (!backward && getParentActivity() != null) {
                navigationBarAnimationColorFrom = getParentActivity().getWindow().getNavigationBarColor();
            }
        }
        transitionAnimationInProress = true;
        checkPhotoDescriptionAlpha();
    }

    public float getInternalTranslationX() {
        return listView != null ? listView.getTranslationX() : 0;
    }

    public float getInternalVisibility() {
        return listView != null ? listView.getAlpha() : 0;
    }


    @Override
    public void onTransitionAnimationEnd(boolean isOpen, boolean backward) {
        if (isOpen) {
            if (!backward) {
                if (playProfileAnimation != 0 && allowProfileAnimation) {
                    if (playProfileAnimation == 1) {
                        currentExpandAnimatorValue = 0f;
                        if (ratingView != null) {
                            ratingView.setParentExpanded(0);
                        }
                        if (actionsView != null) {
                            actionsView.setParentExpanded(0);
                        }
                        if (musicView != null) {
                            musicView.setParentExpanded(0);
                        }
                    }
                    openAnimationInProgress = false;
                    checkListViewScroll();
                    if (recreateMenuAfterAnimation) {
                        createActionBarMenu(true);
                    }
                }
                if (!fragmentOpened) {
                    fragmentOpened = true;
                    invalidateScroll = true;
                    fragmentView.requestLayout();
                }
            }
            getNotificationCenter().onAnimationFinish(transitionIndex);

            if (blurredView != null && blurredView.getVisibility() == View.VISIBLE) {
                blurredView.setVisibility(View.GONE);
                blurredView.setBackground(null);
            }
        }
        transitionAnimationInProress = false;
        checkPhotoDescriptionAlpha();
    }

    @Keep
    public float getAvatarAnimationProgress() {
        return avatarAnimationProgress;
    }

    private AboutLinkCell aboutLinkCell;

    @Keep
    public void setAvatarAnimationProgress(float progress) {
        avatarAnimationProgress = currentExpandAnimatorValue = progress;
        checkPhotoDescriptionAlpha();
        if (playProfileAnimation == 2) {
            avatarImage.setProgressToExpand(progress);
            if (actionsView != null) {
                actionsView.setParentExpanded(progress);
            }
            if (musicView != null) {
                musicView.setParentExpanded(progress);
            }
            if (ratingView != null) {
                ratingView.setParentExpanded(progress);
            }
            updateActionsPosition();
            updateMusicPosition();
        }

        listView.setAlpha(progress);

        listView.setTranslationX(dp(48) - dp(48) * progress);

        int color;
        if (playProfileAnimation == 2 && avatarColor != 0) {
            color = avatarColor;
        } else {
            color = AvatarDrawable.getProfileBackColorForId(userId != 0 || ChatObject.isChannel(chatId, currentAccount) && !currentChat.megagroup ? 5 : chatId, resourcesProvider);
        }

        int actionBarColor = actionBarAnimationColorFrom != 0 ? actionBarAnimationColorFrom : getThemedColor(Theme.key_actionBarDefault);
        int actionBarColor2 = actionBarColor;
        if (SharedConfig.chatBlurEnabled()) {
            actionBarColor = ColorUtils.setAlphaComponent(actionBarColor, 0);
        }
        topView.setBackgroundColor(ColorUtils.blendARGB(actionBarColor, color, progress));
        timerDrawable.setBackgroundColor(ColorUtils.blendARGB(actionBarColor2, color, progress));

        color = peerColor != null ? Color.WHITE : AvatarDrawable.getIconColorForId(userId != 0 || ChatObject.isChannel(chatId, currentAccount) && !currentChat.megagroup ? 5 : chatId, resourcesProvider);
        int iconColor = getThemedColor(Theme.key_actionBarDefaultIcon);
        actionBar.setItemsColor(ColorUtils.blendARGB(iconColor, color, avatarAnimationProgress), false);

        color = peerColor != null ? Color.WHITE : getThemedColor(Theme.key_profile_title);
        int titleColor = /*peerColor != null ? Color.WHITE :*/ getThemedColor(Theme.key_actionBarDefaultTitle);
        for (int i = 0; i < 2; i++) {
            if (nameTextView[i] == null || i == 1 && playProfileAnimation == 2) {
                continue;
            }
            nameTextView[i].setTextColor(ColorUtils.blendARGB(titleColor, color, progress));
        }

        color = isOnline[0] ? getThemedColor(Theme.key_profile_status) : AvatarDrawable.getProfileTextColorForId(userId != 0 || ChatObject.isChannel(chatId, currentAccount) && !currentChat.megagroup ? 5 : chatId, resourcesProvider);
        int subtitleColor = getThemedColor(isOnline[0] ? Theme.key_chat_status : Theme.key_actionBarDefaultSubtitle);
        for (int i = 0; i < 3; i++) {
            if (onlineTextView[i] == null || i == 1 || i == 2 && playProfileAnimation == 2) {
                continue;
            }
            onlineTextView[i].setTextColor(ColorUtils.blendARGB(i == 0 ? subtitleColor : applyPeerColor(subtitleColor, true, isOnline[0]), i == 0 ? color : applyPeerColor(color, true, isOnline[0]), progress));
        }
        extraHeight = initialAnimationExtraHeight * progress;
        color = AvatarDrawable.getProfileColorForId(userId != 0 ? userId : chatId, resourcesProvider);
        int color2 = AvatarDrawable.getColorForId(userId != 0 ? userId : chatId);
        if (color != color2) {
            avatarDrawable.setColor(ColorUtils.blendARGB(color2, color, progress));
            avatarImage.invalidate();
        }

        if (navigationBarAnimationColorFrom != 0) {
            color = ColorUtils.blendARGB(navigationBarAnimationColorFrom, getNavigationBarColor(), progress);
            setNavigationBarColor(color);
        }

        topView.invalidate();

        needLayout(true);
        if (fragmentView != null) {
            fragmentView.invalidate();
        }

        if (aboutLinkCell != null) {
            aboutLinkCell.invalidate();
        }

        if (getDialogId() > 0) {
            if (avatarImage != null) {
                avatarImage.setProgressToStoriesInsets(avatarAnimationProgress);
            }
            if (storyView != null) {
                storyView.setProgressToStoriesInsets(avatarAnimationProgress);
            }
            if (giftsView != null) {
                giftsView.setProgressToStoriesInsets(avatarAnimationProgress);
            }
        }
    }

    boolean profileTransitionInProgress;

    @Override
    public AnimatorSet onCustomTransitionAnimation(final boolean isOpen, final Runnable callback) {
        if (hasMainTabs) {
            return null;
        }

        updateRowsIds();

        if (playProfileAnimation != 0 && allowProfileAnimation && !isPulledDown && !disableProfileAnimation) {
            if (timeItem != null) {
                timeItem.setAlpha(1.0f);
            }
            if (starFgItem != null) {
                starFgItem.setAlpha(1.0f);
                starFgItem.setScaleX(1.0f);
                starFgItem.setScaleY(1.0f);
            }
            if (starBgItem != null) {
                starBgItem.setAlpha(1.0f);
                starBgItem.setScaleX(1.0f);
                starBgItem.setScaleY(1.0f);
            }
            previousTransitionMainFragment = null;
            if (parentLayout != null && parentLayout.getFragmentStack().size() >= 2) {
                BaseFragment fragment = parentLayout.getFragmentStack().get(parentLayout.getFragmentStack().size() - 2);
                if (fragment instanceof ChatActivityInterface) {
                    previousTransitionFragment = (ChatActivityInterface) fragment;
                }
                if (fragment instanceof DialogsActivity) {
                    DialogsActivity dialogsActivity = (DialogsActivity) fragment;
                    if (dialogsActivity.rightSlidingDialogContainer != null && dialogsActivity.rightSlidingDialogContainer.currentFragment instanceof ChatActivityInterface) {
                        previousTransitionMainFragment = dialogsActivity;
                        previousTransitionFragment = (ChatActivityInterface) dialogsActivity.rightSlidingDialogContainer.currentFragment;
                    }
                }
            }
            final boolean fromChat = previousTransitionFragment instanceof ChatActivity && ((ChatActivity) previousTransitionFragment).getCurrentChat() != null;
            if (previousTransitionFragment != null) {
                updateTimeItem();
                updateStar();
            }
            final AnimatorSet animatorSet = new AnimatorSet();
            animatorSet.setDuration(playProfileAnimation == 2 ? 250 : 180);
            listView.setLayerType(View.LAYER_TYPE_HARDWARE, null);
            ActionBarMenu menu = actionBar.createMenu();
            if (menu.getItem(10) == null) {
                if (animatingItem == null) {
                    animatingItem = menu.addItem(10, R.drawable.ic_ab_other);
                }
            }

            if (isOpen) {
                for (int i = 0; i < 2; i++) {
                    FrameLayout.LayoutParams layoutParams = (FrameLayout.LayoutParams) onlineTextView[i + 1].getLayoutParams();
                    layoutParams.rightMargin = (int) (-21 * AndroidUtilities.density + AndroidUtilities.dp(8));
                    onlineTextView[i + 1].setLayoutParams(layoutParams);
                }

                if (playProfileAnimation != 2) {
                    int width = (int) Math.ceil(AndroidUtilities.displaySize.x - AndroidUtilities.dp(118 + 8) + 21 * AndroidUtilities.density);
                    float width2 = nameTextView[1].getPaint().measureText(nameTextView[1].getText().toString()) * 1.12f + nameTextView[1].getSideDrawablesSize();
                    FrameLayout.LayoutParams layoutParams = (FrameLayout.LayoutParams) nameTextView[1].getLayoutParams();
                    if (width < width2) {
                        layoutParams.width = (int) Math.ceil(width / 1.12f);
                    } else {
                        layoutParams.width = LayoutHelper.WRAP_CONTENT;
                    }
                    nameTextView[1].setLayoutParams(layoutParams);

                    initialAnimationExtraHeight = getHeaderExtraHeight();
                } else {
                    FrameLayout.LayoutParams layoutParams = (FrameLayout.LayoutParams) nameTextView[1].getLayoutParams();
                    layoutParams.width = (int) ((AndroidUtilities.displaySize.x - dp(32)) / 1.38f);
                    nameTextView[1].setLayoutParams(layoutParams);
                }
                fragmentView.setBackgroundColor(0);
                setAvatarAnimationProgress(0);
                ArrayList<Animator> animators = new ArrayList<>();
                animators.add(ObjectAnimator.ofFloat(this, "avatarAnimationProgress", 0.0f, 1.0f));
                if (writeButton != null && writeButton.getTag() == null) {
                    writeButton.setScaleX(0.2f);
                    writeButton.setScaleY(0.2f);
                    writeButton.setAlpha(0.0f);
                    animators.add(ObjectAnimator.ofFloat(writeButton, View.SCALE_X, 1.0f));
                    animators.add(ObjectAnimator.ofFloat(writeButton, View.SCALE_Y, 1.0f));
                    animators.add(ObjectAnimator.ofFloat(writeButton, View.ALPHA, 1.0f));
                }
                if (storyView != null && playProfileAnimation == 2) {
                    storyView.setAlpha(0.0f);
                    animators.add(ObjectAnimator.ofFloat(storyView, View.ALPHA, 1.0f));
                }
                if (playProfileAnimation == 2) {
                    avatarColor = getAverageColor(avatarImage.getImageReceiver());
                    nameTextView[1].setTextColor(Color.WHITE);
                    onlineTextView[1].setTextColor(0xB3FFFFFF);
                    actionBar.setItemsBackgroundColor(Theme.ACTION_BAR_WHITE_SELECTOR_COLOR, false);
                    if (showStatusButton != null) {
                        showStatusButton.setBackgroundColor(0x23ffffff);
                    }
                    overlaysView.setOverlaysVisible();
                }
                for (int a = 0; a < 2; a++) {
                    nameTextView[a].setAlpha(a == 0 ? 1.0f : 0.0f);
                    animators.add(ObjectAnimator.ofFloat(nameTextView[a], View.ALPHA, a == 0 ? 0.0f : 1.0f));
                }
                if (storyView != null) {
                    if (getDialogId() > 0) {
                        storyView.setAlpha(0f);
                        animators.add(ObjectAnimator.ofFloat(storyView, View.ALPHA, 1.0f));
                    } else {
                        storyView.setAlpha(1f);
                        storyView.setFragmentTransitionProgress(0);
                        animators.add(ObjectAnimator.ofFloat(storyView, ProfileStoriesView.FRAGMENT_TRANSITION_PROPERTY, 1.0f));
                    }
                }
                if (giftsView != null) {
                    giftsView.setAlpha(0f);
                    animators.add(ObjectAnimator.ofFloat(giftsView, View.ALPHA, 1.0f));
                }
                if (timeItem.getTag() != null) {
                    animators.add(ObjectAnimator.ofFloat(timeItem, View.ALPHA, 1.0f, 0.0f));
                    animators.add(ObjectAnimator.ofFloat(timeItem, View.SCALE_X, 1.0f, 0.0f));
                    animators.add(ObjectAnimator.ofFloat(timeItem, View.SCALE_Y, 1.0f, 0.0f));
                }
                if (starFgItem.getTag() != null) {
                    animators.add(ObjectAnimator.ofFloat(starFgItem, View.ALPHA, 1.0f, 0.0f));
                    animators.add(ObjectAnimator.ofFloat(starFgItem, View.SCALE_X, 1.0f, 0.0f));
                    animators.add(ObjectAnimator.ofFloat(starFgItem, View.SCALE_Y, 1.0f, 0.0f));
                }
                if (starBgItem.getTag() != null) {
                    animators.add(ObjectAnimator.ofFloat(starBgItem, View.ALPHA, 1.0f, 0.0f));
                    animators.add(ObjectAnimator.ofFloat(starBgItem, View.SCALE_X, 1.0f, 0.0f));
                    animators.add(ObjectAnimator.ofFloat(starBgItem, View.SCALE_Y, 1.0f, 0.0f));
                }
                if (animatingItem != null) {
                    animatingItem.setAlpha(1.0f);
                    animators.add(ObjectAnimator.ofFloat(animatingItem, View.ALPHA, 0.0f));
                }
                if (callItemVisible && (chatId != 0 || fromChat)) {
                    callItem.setAlpha(0.0f);
                    animators.add(ObjectAnimator.ofFloat(callItem, View.ALPHA, 1.0f));
                }
                if (videoCallItemVisible) {
                    videoCallItem.setAlpha(0.0f);
                    animators.add(ObjectAnimator.ofFloat(videoCallItem, View.ALPHA, 1.0f));
                }
                if (editItemVisible) {
                    editItem.setAlpha(0.0f);
                    animators.add(ObjectAnimator.ofFloat(editItem, View.ALPHA, 1.0f));
                }
                if (ttlIconView.getTag() != null) {
                    ttlIconView.setAlpha(0f);
                    animators.add(ObjectAnimator.ofFloat(ttlIconView, View.ALPHA, 1.0f));
                }
                if (avatarImage != null) {
                    avatarImage.setCrossfadeProgress(1.0f);
                    animators.add(ObjectAnimator.ofFloat(avatarImage, AvatarImageView.CROSSFADE_PROGRESS, 0.0f));
                }

                boolean onlineTextCrosafade = false;

                if (previousTransitionFragment != null) {
                    ChatAvatarContainer avatarContainer = previousTransitionFragment.getAvatarContainer();
                    if (avatarContainer != null && avatarContainer.getSubtitleTextView() instanceof SimpleTextView && ((SimpleTextView) avatarContainer.getSubtitleTextView()).getLeftDrawable() != null || avatarContainer.statusMadeShorter[0]) {
                        transitionOnlineText = avatarContainer.getSubtitleTextView();
                        avatarContainer2.invalidate();
                        onlineTextCrosafade = true;
                        onlineTextView[0].setAlpha(0f);
                        onlineTextView[1].setAlpha(0f);
                        animators.add(ObjectAnimator.ofFloat(onlineTextView[1], View.ALPHA, 1.0f));
                    }
                }

                if (!onlineTextCrosafade) {
                    for (int a = 0; a < 2; a++) {
                        onlineTextView[a].setAlpha(a == 0 ? 1.0f : 0.0f);
                        animators.add(ObjectAnimator.ofFloat(onlineTextView[a], View.ALPHA, a == 0 ? 0.0f : 1.0f));
                    }
                }
                animatorSet.playTogether(animators);
            } else {
                initialAnimationExtraHeight = extraHeight;

                ArrayList<Animator> animators = new ArrayList<>();
                animators.add(ObjectAnimator.ofFloat(this, "avatarAnimationProgress", 1.0f, 0.0f));
                if (writeButton != null) {
                    animators.add(ObjectAnimator.ofFloat(writeButton, View.SCALE_X, 0.2f));
                    animators.add(ObjectAnimator.ofFloat(writeButton, View.SCALE_Y, 0.2f));
                    animators.add(ObjectAnimator.ofFloat(writeButton, View.ALPHA, 0.0f));
                }
                for (int a = 0; a < 2; a++) {
                    animators.add(ObjectAnimator.ofFloat(nameTextView[a], View.ALPHA, a == 0 ? 1.0f : 0.0f));
                }
                if (storyView != null) {
                    if (dialogId > 0) {
                        animators.add(ObjectAnimator.ofFloat(storyView, View.ALPHA, 0.0f));
                    } else {
                        animators.add(ObjectAnimator.ofFloat(storyView, ProfileStoriesView.FRAGMENT_TRANSITION_PROPERTY, 0.0f));
                    }
                }
                if (giftsView != null) {
                    animators.add(ObjectAnimator.ofFloat(giftsView, View.ALPHA, 0.0f));
                }
                if (timeItem.getTag() != null) {
                    timeItem.setAlpha(0f);
                    animators.add(ObjectAnimator.ofFloat(timeItem, View.ALPHA, 0.0f, 1.0f));
                    animators.add(ObjectAnimator.ofFloat(timeItem, View.SCALE_X, 0.0f, 1.0f));
                    animators.add(ObjectAnimator.ofFloat(timeItem, View.SCALE_Y, 0.0f, 1.0f));
                }
                if (starFgItem.getTag() != null) {
                    starFgItem.setAlpha(0f);
                    animators.add(ObjectAnimator.ofFloat(starFgItem, View.ALPHA, 0.0f, 1.0f));
                    animators.add(ObjectAnimator.ofFloat(starFgItem, View.SCALE_X, 0.0f, 1.0f));
                    animators.add(ObjectAnimator.ofFloat(starFgItem, View.SCALE_Y, 0.0f, 1.0f));
                }
                if (starBgItem.getTag() != null) {
                    starBgItem.setAlpha(0f);
                    animators.add(ObjectAnimator.ofFloat(starBgItem, View.ALPHA, 0.0f, 1.0f));
                    animators.add(ObjectAnimator.ofFloat(starBgItem, View.SCALE_X, 0.0f, 1.0f));
                    animators.add(ObjectAnimator.ofFloat(starBgItem, View.SCALE_Y, 0.0f, 1.0f));
                }
                if (animatingItem != null) {
                    animatingItem.setAlpha(0.0f);
                    animators.add(ObjectAnimator.ofFloat(animatingItem, View.ALPHA, 1.0f));
                }
                if (callItemVisible && (chatId != 0 || fromChat)) {
                    callItem.setAlpha(1.0f);
                    animators.add(ObjectAnimator.ofFloat(callItem, View.ALPHA, 0.0f));
                }
                if (videoCallItemVisible) {
                    videoCallItem.setAlpha(1.0f);
                    animators.add(ObjectAnimator.ofFloat(videoCallItem, View.ALPHA, 0.0f));
                }
                if (editItemVisible) {
                    editItem.setAlpha(1.0f);
                    animators.add(ObjectAnimator.ofFloat(editItem, View.ALPHA, 0.0f));
                }
                if (ttlIconView != null) {
                    animators.add(ObjectAnimator.ofFloat(ttlIconView, View.ALPHA, ttlIconView.getAlpha(), 0.0f));
                }
                if (avatarImage != null) {
                    animators.add(ObjectAnimator.ofFloat(avatarImage, AvatarImageView.CROSSFADE_PROGRESS, 1.0f));
                }

                boolean crossfadeOnlineText = false;
                BaseFragment previousFragment = parentLayout.getFragmentStack().size() > 1 ? parentLayout.getFragmentStack().get(parentLayout.getFragmentStack().size() - 2) : null;
                if (previousFragment instanceof ChatActivity) {
                    ChatAvatarContainer avatarContainer = ((ChatActivity) previousFragment).getAvatarContainer();
                    View subtitleTextView = avatarContainer.getSubtitleTextView();
                    if (subtitleTextView instanceof SimpleTextView && ((SimpleTextView) subtitleTextView).getLeftDrawable() != null || avatarContainer.statusMadeShorter[0]) {
                        transitionOnlineText = avatarContainer.getSubtitleTextView();
                        avatarContainer2.invalidate();
                        crossfadeOnlineText = true;
                        animators.add(ObjectAnimator.ofFloat(onlineTextView[0], View.ALPHA, 0.0f));
                        animators.add(ObjectAnimator.ofFloat(onlineTextView[1], View.ALPHA, 0.0f));
                    }
                }
                if (!crossfadeOnlineText) {
                    for (int a = 0; a < 2; a++) {
                        animators.add(ObjectAnimator.ofFloat(onlineTextView[a], View.ALPHA, a == 0 ? 1.0f : 0.0f));
                    }
                }
                animatorSet.playTogether(animators);
                if (birthdayEffect != null) {
                    birthdayEffect.hide();
                }
            }
            profileTransitionInProgress = true;
            ValueAnimator valueAnimator = ValueAnimator.ofFloat(0, 1f);
            valueAnimator.addUpdateListener(valueAnimator1 -> {
                if (fragmentView != null) {
                    fragmentView.invalidate();
                }
                updateStoriesViewBounds(true);
            });
            animatorSet.playTogether(valueAnimator);

            ActionBarMenuItem prevCallItem = null;
            if (callToActionItem != null && previousTransitionFragment != null) {
                ActionBarMenu prevMenu = previousTransitionFragment.getActionBar().menu;
                if (prevMenu != null) {
                    prevCallItem = prevMenu.getItem(32);
                    if (prevCallItem != null && prevCallItem.getVisibility() == View.VISIBLE) {
                        prevCallItem.setAlpha(0f);
                        if (isOpen) {
                            callToActionItem.setVisibility(View.VISIBLE);
                        }
                        callToActionItem.setTag(prevCallItem);
                    } else {
                        prevCallItem = null;
                    }
                }
            }

            final ActionBarMenuItem finalPrevCallItem = prevCallItem;
            animatorSet.addListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    if (finalPrevCallItem != null) {
                        finalPrevCallItem.setAlpha(1f);
                    }
                    if (fragmentView == null) {
                        callback.run();
                        return;
                    }
                    avatarImage.setProgressToExpand(0);
                    listView.setLayerType(View.LAYER_TYPE_NONE, null);
                    if (animatingItem != null) {
                        ActionBarMenu menu = actionBar.createMenu();
                        menu.clearItems();
                        animatingItem = null;
                    }
                    callback.run();
                    if (playProfileAnimation == 2) {
                        playProfileAnimation = 1;
                        avatarImage.setForegroundAlpha(1.0f);
                        avatarContainer.setVisibility(View.GONE);
                        avatarsViewPager.setAlpha(1f);
                        avatarsViewPager.resetCurrentItem();
                        avatarsViewPager.setVisibility(View.VISIBLE);
                    }
                    transitionOnlineText = null;
                    avatarContainer2.invalidate();
                    profileTransitionInProgress = false;
                    previousTransitionFragment = null;
                    previousTransitionMainFragment = null;
                    fragmentView.invalidate();
                }
            });
            animatorSet.setInterpolator(playProfileAnimation == 2 ? CubicBezierInterpolator.DEFAULT : new DecelerateInterpolator());

            AndroidUtilities.runOnUIThread(animatorSet::start, 50);
            return animatorSet;
        }
        return null;
    }

    private int getAverageColor(ImageReceiver imageReceiver) {
        if (imageReceiver.getDrawable() instanceof VectorAvatarThumbDrawable) {
            return ((VectorAvatarThumbDrawable) imageReceiver.getDrawable()).gradientTools.getAverageColor();
        }
        return AndroidUtilities.calcBitmapColor(avatarImage.getImageReceiver().getBitmap());
    }

    private void updateOnlineCount(boolean notify) {
        onlineCount = 0;
        int currentTime = getConnectionsManager().getCurrentTime();
        sortedUsers.clear();
        if (chatInfo instanceof TLRPC.TL_chatFull || chatInfo instanceof TLRPC.TL_channelFull && chatInfo.participants_count <= 200 && chatInfo.participants != null) {
            final ArrayList<Integer> sortNum = new ArrayList<>();
            for (int a = 0; a < chatInfo.participants.participants.size(); a++) {
                TLRPC.ChatParticipant participant = chatInfo.participants.participants.get(a);
                TLRPC.User user = getMessagesController().getUser(participant.user_id);
                if (user != null && user.status != null && (user.status.expires > currentTime || user.id == getUserConfig().getClientUserId()) && user.status.expires > 10000) {
                    onlineCount++;
                }
                sortedUsers.add(a);
                int sort = Integer.MIN_VALUE;
                if (user != null) {
                    if (user.bot) {
                        sort = -110;
                    } else if (user.self) {
                        sort = currentTime + 50000;
                    } else if (user.status != null) {
                        sort = user.status.expires;
                    }
                }
                sortNum.add(sort);
            }

            try {
                Collections.sort(sortedUsers, Comparator.comparingInt(hs -> sortNum.get((int) hs)).reversed());
            } catch (Exception e) {
                FileLog.e(e);
            }

            if (notify && listAdapter != null && membersStartRow > 0) {
                AndroidUtilities.updateVisibleRows(listView);
            }
            if (sharedMediaLayout != null && sharedMediaRow != -1 && (sortedUsers.size() > 5 || usersForceShowingIn == 2) && usersForceShowingIn != 1) {
                sharedMediaLayout.setChatUsers(sortedUsers, chatInfo);
            }
        } else if (chatInfo instanceof TLRPC.TL_channelFull && chatInfo.participants_count > 200) {
            onlineCount = chatInfo.online_count;
        }
    }

    public void setChatInfo(TLRPC.ChatFull value) {
        chatInfo = value;
        if (chatInfo != null && chatInfo.migrated_from_chat_id != 0 && mergeDialogId == 0) {
            mergeDialogId = -chatInfo.migrated_from_chat_id;
            getMediaDataController().getMediaCounts(mergeDialogId, topicId, classGuid);
        }
        if (sharedMediaLayout != null) {
            sharedMediaLayout.setChatInfo(chatInfo);
        }
        if (avatarsViewPager != null && !isTopic) {
            avatarsViewPager.setChatInfo(chatInfo);
        }
        if (storyView != null && chatInfo != null) {
            storyView.setStories(chatInfo.stories);
        }
        if (giftsView != null) {
            giftsView.update();
        }
        if (avatarImage != null) {
            avatarImage.setHasStories(needInsetForStories());
        }
        fetchUsersFromChannelInfo();
        if (chatId != 0) {
            boolean gift = !BuildVars.IS_BILLING_UNAVAILABLE && !getMessagesController().premiumPurchaseBlocked() && chatInfo != null && chatInfo.stargifts_available;
            otherItem.setSubItemShown(gift_premium, gift);
            if (actionsView != null) {
                actionsView.set(ProfileActionsView.KEY_GIFT, gift);
            }
        }
    }

    private boolean needInsetForStories() {
        return getMessagesController().getStoriesController().hasStories(getDialogId()) && !isTopic;
    }

    public void setUserInfo(
            TLRPC.UserFull value,
            ProfileChannelCell.ChannelMessageFetcher channelMessageFetcher,
            ProfileBirthdayEffect.BirthdayEffectFetcher birthdayAssetsFetcher
    ) {
        userInfo = value;
        if (ratingView != null) {
            ratingView.set(userInfo.stars_rating);
        }
        if (storyView != null) {
            storyView.setStories(userInfo.stories);
        }
        if (giftsView != null) {
            giftsView.update();
        }
        if (avatarImage != null) {
            avatarImage.setHasStories(needInsetForStories());
        }
        if (sharedMediaLayout != null) {
            sharedMediaLayout.setUserInfo(userInfo);
        }
        if (profileChannelMessageFetcher == null) {
            profileChannelMessageFetcher = channelMessageFetcher;
        }
        if (profileChannelMessageFetcher == null) {
            profileChannelMessageFetcher = new ProfileChannelCell.ChannelMessageFetcher(currentAccount);
        }
        profileChannelMessageFetcher.subscribe(() -> updateListAnimated(false));
        profileChannelMessageFetcher.fetch(userInfo);
        if (birthdayFetcher == null) {
            birthdayFetcher = birthdayAssetsFetcher;
        }
        if (birthdayFetcher == null) {
            birthdayFetcher = ProfileBirthdayEffect.BirthdayEffectFetcher.of(currentAccount, userInfo, birthdayFetcher);
            createdBirthdayFetcher = birthdayFetcher != null;
        }
        if (birthdayFetcher != null) {
            birthdayFetcher.subscribe(this::createBirthdayEffect);
        }
        if (otherItem != null) {
            otherItem.setSubItemShown(start_secret_chat, DialogObject.isEmpty(getMessagesController().isUserContactBlocked(userId)));
            if (hasPrivacyCommand()) {
                otherItem.showSubItem(bot_privacy);
            } else {
                otherItem.hideSubItem(bot_privacy);
            }
        }
    }

    public boolean canSearchMembers() {
        return canSearchMembers;
    }

    private void fetchUsersFromChannelInfo() {
        if (currentChat == null || !currentChat.megagroup) {
            return;
        }
        if (chatInfo instanceof TLRPC.TL_channelFull && chatInfo.participants != null) {
            for (int a = 0; a < chatInfo.participants.participants.size(); a++) {
                TLRPC.ChatParticipant chatParticipant = chatInfo.participants.participants.get(a);
                participantsMap.put(chatParticipant.user_id, chatParticipant);
            }
        }
    }

    private void kickUser(long uid, TLRPC.ChatParticipant participant) {
        if (uid != 0) {
            TLRPC.User user = getMessagesController().getUser(uid);
            getMessagesController().deleteParticipantFromChat(chatId, user);
            if (currentChat != null && user != null && BulletinFactory.canShowBulletin(this)) {
                BulletinFactory.createRemoveFromChatBulletin(this, user, currentChat.title).show();
            }
            if (chatInfo.participants.participants.remove(participant)) {
                updateListAnimated(true);
            }
        } else {
            getNotificationCenter().removeObserver(this, NotificationCenter.closeChats);
            if (AndroidUtilities.isTablet()) {
                getNotificationCenter().postNotificationName(NotificationCenter.closeChats, -chatId);
            } else {
                getNotificationCenter().postNotificationName(NotificationCenter.closeChats);
            }
            getMessagesController().deleteParticipantFromChat(chatId, getMessagesController().getUser(getUserConfig().getClientUserId()));
            playProfileAnimation = 0;
            finishFragment();
        }
    }

    public boolean isChat() {
        return chatId != 0;
    }

    private void updateRowsIds() {
        updateNotifications(false);

        boolean hasJoinRow = false;

        int prevRowsCount = rowCount;
        rowCount = 0;

        setAvatarRow = -1;
        setAvatarSectionRow = -1;
        numberSectionRow = -1;
        numberRow = -1;
        birthdayRow = -1;
        setUsernameRow = -1;
        bioRow = -1;
        channelRow = -1;
        channelDividerRow = -1;
        phoneSuggestionSectionRow = -1;
        phoneSuggestionRow = -1;
        passwordSuggestionSectionRow = -1;
        graceSuggestionRow = -1;
        graceSuggestionSectionRow = -1;
        passwordSuggestionRow = -1;
        settingsSectionRow = -1;
        settingsSectionRow2 = -1;
        notificationRow = -1;
        languageRow = -1;
        premiumRow = -1;
        starsRow = -1;
        tonRow = -1;
        businessRow = -1;
        premiumGiftingRow = -1;
        premiumSectionsRow = -1;
        privacyRow = -1;
        dataRow = -1;
        chatRow = -1;
        filtersRow = -1;
        liteModeRow = -1;
        stickersRow = -1;
        devicesRow = -1;
        devicesSectionRow = -1;
        helpHeaderRow = -1;
        questionRow = -1;
        faqRow = -1;
        policyRow = -1;
        helpSectionCell = -1;
        debugHeaderRow = -1;
        sendLogsRow = -1;
        sendLastLogsRow = -1;
        clearLogsRow = -1;
        switchBackendRow = -1;
        versionRow = -1;
        botAppRow = -1;
        botPermissionsHeader = -1;
        botPermissionBiometry = -1;
        botPermissionEmojiStatus = -1;
        botPermissionLocation = -1;
        botPermissionsDivider = -1;
        unofficialSecurityRiskRow = -1;
        unofficialSecurityRiskDividerRow = -1;

        sendMessageRow = -1;
        reportRow = -1;
        reportReactionRow = -1;
        deleteReactionRow = -1;
        addToContactsRow = -1;
        emptyRow = -1;
        emptyRow2 = -1;
        infoHeaderRow = -1;
        infoHeaderRowEmpty = -1;
        infoEndRowEmpty = -1;
        phoneRow = -1;
        noteRow = -1;
        userInfoRow = -1;
        locationRow = -1;
        channelInfoRow = -1;
        usernameRow = -1;
        settingsTimerRow = -1;
        settingsKeyRow = -1;
        notificationsDividerRow = -1;
        reportDividerRow = -1;
        notificationsRow = -1;
        bizLocationRow = -1;
        bizHoursRow = -1;
        infoSectionRow = -1;
        affiliateRow = -1;
        infoAffiliateRow = -1;
        secretSettingsSectionRow = -1;
        bottomPaddingRow = -1;
        addToGroupButtonRow = -1;
        addToGroupInfoRow = -1;
        infoStartRow = -1;
        infoEndRow = -1;

        membersHeaderRow = -1;
        membersStartRow = -1;
        membersEndRow = -1;
        addMemberRow = -1;
        subscribersRow = -1;
        subscribersRequestsRow = -1;
        administratorsRow = -1;
        blockedUsersRow = -1;
        membersSectionRow = -1;
        channelBalanceSectionRow = -1;
        sharedMediaRow = -1;
        notificationsSimpleRow = -1;
        settingsRow = -1;
        botStarsBalanceRow = -1;
        botTonBalanceRow = -1;
        channelBalanceRow = -1;
        balanceDividerRow = -1;
        hasMusic = false;

        unblockRow = -1;
        joinRow = -1;
        lastSectionRow = -1;
        visibleChatParticipants.clear();
        visibleSortedUsers.clear();

        boolean hasMedia = false;
        if (sharedMediaPreloader != null) {
            int[] lastMediaCount = sharedMediaPreloader.getLastMediaCount();
            for (int a = 0; a < lastMediaCount.length; a++) {
                if (lastMediaCount[a] > 0) {
                    hasMedia = true;
                    break;
                }
            }
            if (!hasMedia) {
                hasMedia = sharedMediaPreloader.hasSavedMessages;
            }
            if (!hasMedia) {
                hasMedia = sharedMediaPreloader.hasPreviews;
            }
        }
        if (!hasMedia && userInfo != null) {
            hasMedia = userInfo.stories_pinned_available;
        }
        if (!hasMedia && userInfo != null && userInfo.bot_info != null) {
            hasMedia = userInfo.bot_info.has_preview_medias;
        }
        if (!hasMedia && (userInfo != null && userInfo.stargifts_count > 0 || chatInfo != null && chatInfo.stargifts_count > 0)) {
            hasMedia = true;
        }
        if (!hasMedia && chatInfo != null) {
            hasMedia = chatInfo.stories_pinned_available;
        }
        if (!hasMedia) {
            if (chatId != 0 && MessagesController.ChannelRecommendations.hasRecommendations(currentAccount, -chatId)) {
                hasMedia = true;
            } else if (isBot && userId != 0 && MessagesController.ChannelRecommendations.hasRecommendations(currentAccount, userId)) {
                hasMedia = true;
            }
        }

        if (userId != 0) {
            TLRPC.User user = getMessagesController().getUser(userId);
            if (userInfo != null && userInfo.saved_music != null && (imageUpdater == null || myProfile)) {
                hasMusic = true;
            }

            if (emptyRow < 0 && emptyRow2 < 0) {
                if (hasMusic || peerColor != null || actionsView == null) {
                    emptyRow2 = rowCount++;
                } else {
                    emptyRow = rowCount++;
                }
            }

            if (UserObject.isUserSelf(user) && !myProfile) {
                if (avatarBig == null && (user.photo == null || !(user.photo.photo_big instanceof TLRPC.TL_fileLocation_layer97) && !(user.photo.photo_big instanceof TLRPC.TL_fileLocationToBeDeprecated)) && (avatarsViewPager == null || avatarsViewPager.getRealCount() == 0)) {
                    setAvatarRow = rowCount++;
                    setAvatarSectionRow = rowCount++;
                }
                numberSectionRow = rowCount++;
                numberRow = rowCount++;
                setUsernameRow = rowCount++;
                bioRow = rowCount++;

                settingsSectionRow = rowCount++;

                Set<String> suggestions = getMessagesController().pendingSuggestions;
                if (suggestions.contains("PREMIUM_GRACE")) {
                    graceSuggestionRow = rowCount++;
                    graceSuggestionSectionRow = rowCount++;
                } else if (suggestions.contains("VALIDATE_PHONE_NUMBER")) {
                    phoneSuggestionRow = rowCount++;
                    phoneSuggestionSectionRow = rowCount++;
                } else if (suggestions.contains("VALIDATE_PASSWORD")) {
                    passwordSuggestionRow = rowCount++;
                    passwordSuggestionSectionRow = rowCount++;
                }

                settingsSectionRow2 = rowCount++;
                chatRow = rowCount++;
                privacyRow = rowCount++;
                notificationRow = rowCount++;
                dataRow = rowCount++;
                liteModeRow = rowCount++;
//                stickersRow = rowCount++;
                if (getMessagesController().filtersEnabled || !getMessagesController().dialogFilters.isEmpty()) {
                    filtersRow = rowCount++;
                }
                devicesRow = rowCount++;
                languageRow = rowCount++;
                devicesSectionRow = rowCount++;
                if (!getMessagesController().premiumFeaturesBlocked()) {
                    premiumRow = rowCount++;
                }
                if (getMessagesController().starsPurchaseAvailable()) {
                    starsRow = rowCount++;
                }
                StarsController.getInstance(currentAccount, true).getBalance();
                if (ApplicationLoader.isBetaBuild() || ApplicationLoader.isStandaloneBuild() || ApplicationLoader.isHuaweiStoreBuild() || (StarsController.getInstance(currentAccount, true).balanceAvailable() && (StarsController.getInstance(currentAccount, true).hasTransactions() || StarsController.getInstance(currentAccount, true).getBalance().positive()))) {
                    tonRow = rowCount++;
                }
                if (!getMessagesController().premiumFeaturesBlocked()) {
                    businessRow = rowCount++;
                }
                if (!getMessagesController().premiumPurchaseBlocked()) {
                    premiumGiftingRow = rowCount++;
                }
                if (premiumRow >= 0 || starsRow >= 0 || tonRow >= 0 || businessRow >= 0 || premiumGiftingRow >= 0) {
                    premiumSectionsRow = rowCount++;
                }
                helpHeaderRow = rowCount++;
                questionRow = rowCount++;
                faqRow = rowCount++;
                policyRow = rowCount++;
                if (BuildVars.LOGS_ENABLED || BuildVars.DEBUG_PRIVATE_VERSION) {
                    helpSectionCell = rowCount++;
                    debugHeaderRow = rowCount++;
                }
                if (BuildVars.LOGS_ENABLED) {
                    sendLogsRow = rowCount++;
                    sendLastLogsRow = rowCount++;
                    clearLogsRow = rowCount++;
                }
                if (BuildVars.DEBUG_VERSION) {
                    switchBackendRow = rowCount++;
                }
                versionRow = rowCount++;
            } else {
                String username = UserObject.getPublicUsername(user);
                boolean hasInfo = userInfo != null && !TextUtils.isEmpty(userInfo.about) || user != null && !TextUtils.isEmpty(username);
                boolean hasPhone = user != null && (!TextUtils.isEmpty(user.phone) || !TextUtils.isEmpty(vcardPhone));

                if (!isBot && userInfo != null && userInfo.unofficial_security_risk) {
                    unofficialSecurityRiskRow = rowCount++;
                    unofficialSecurityRiskDividerRow = rowCount++;
                }

                if (userInfo != null && (userInfo.flags2 & 64) != 0 && (profileChannelMessageFetcher == null || !profileChannelMessageFetcher.loaded || !profileChannelMessageFetcher.messageObjects.isEmpty())) {
                    final TLRPC.Chat channel = getMessagesController().getChat(userInfo.personal_channel_id);
                    if (channel != null && (ChatObject.isPublic(channel) || !ChatObject.isNotInChat(channel))) {
                        channelRow = rowCount++;
                        channelDividerRow = rowCount++;
                    }
                }
                infoStartRow = rowCount;
                if (!isBot && (hasPhone || !hasInfo)) {
                    phoneRow = rowCount++;
                }
                if (userInfo != null && !TextUtils.isEmpty(userInfo.about)) {
                    userInfoRow = rowCount++;
                }
                if (user != null && username != null) {
                    usernameRow = rowCount++;
                }
                if (userInfo != null) {
                    if (userInfo.birthday != null) {
                        birthdayRow = rowCount++;
                    }
                    if (userInfo.business_work_hours != null) {
                        bizHoursRow = rowCount++;
                    }
                    if (userInfo.business_location != null) {
                        bizLocationRow = rowCount++;
                    }
                    if (userInfo.note != null) {
                        noteRow = rowCount++;
                    }
                }
                if (actionsView == null && userId != getUserConfig().getClientUserId()) {
                    notificationsRow = rowCount++;
                }
                if (isBot && user != null && user.bot_has_main_app) {
                    botAppRow = rowCount++;
                }
                infoEndRow = rowCount - 1;
                infoSectionRow = rowCount++;

                if (isBot && userInfo != null && userInfo.starref_program != null && (userInfo.starref_program.flags & 2) == 0 && getMessagesController().starrefConnectAllowed) {
                    affiliateRow = rowCount++;
                    infoAffiliateRow = rowCount++;
                }

                if (isBot) {
                    if (botLocation == null && getContext() != null)
                        botLocation = BotLocation.get(getContext(), currentAccount, userId);
                    if (botBiometry == null && getContext() != null)
                        botBiometry = BotBiometry.get(getContext(), currentAccount, userId);
                    final boolean containsPermissionLocation = botLocation != null && botLocation.asked();
                    final boolean containsPermissionBiometry = botBiometry != null && botBiometry.asked();
                    final boolean containsPermissionEmojiStatus = userInfo != null && userInfo.bot_can_manage_emoji_status || SetupEmojiStatusSheet.getAccessRequested(getContext(), currentAccount, userId);

                    if (containsPermissionEmojiStatus || containsPermissionLocation || containsPermissionBiometry) {
                        botPermissionsHeader = rowCount++;
                        if (containsPermissionEmojiStatus) {
                            botPermissionEmojiStatus = rowCount++;
                        }
                        if (containsPermissionLocation) {
                            botPermissionLocation = rowCount++;
                        }
                        if (containsPermissionBiometry) {
                            botPermissionBiometry = rowCount++;
                        }
                        botPermissionsDivider = rowCount++;
                    }
                }

                if (currentEncryptedChat instanceof TLRPC.TL_encryptedChat) {
                    settingsTimerRow = rowCount++;
                    settingsKeyRow = rowCount++;
                    secretSettingsSectionRow = rowCount++;
                }

                if (user != null && !isBot && currentEncryptedChat == null && user.id != getUserConfig().getClientUserId()) {
                    if (userBlocked) {
                        unblockRow = rowCount++;
                        lastSectionRow = rowCount++;
                    }
                }


                boolean divider = false;
                if (user != null && user.bot) {
                    if (userInfo != null && userInfo.can_view_revenue && BotStarsController.getInstance(currentAccount).getTONBalance(userId) > 0) {
                        botTonBalanceRow = rowCount++;
                    }
                    if (BotStarsController.getInstance(currentAccount).getBotStarsBalance(userId).amount > 0 || BotStarsController.getInstance(currentAccount).hasTransactions(userId)) {
                        botStarsBalanceRow = rowCount++;
                    }
                }

                if (user != null && isBot && !user.bot_nochats) {
                    addToGroupButtonRow = rowCount++;
                    addToGroupInfoRow = rowCount++;
                } else if (botStarsBalanceRow >= 0) {
                    divider = true;
                }

                if (!myProfile && showAddToContacts && user != null && !user.contact && !user.bot && !UserObject.isService(user.id)) {
                    addToContactsRow = rowCount++;
                    divider = true;
                }
                if (!myProfile && reportReactionMessageId != 0 && reportReactionFromDialogId < 0) {
                    TLRPC.Chat fromChat = getMessagesController().getChat(-reportReactionFromDialogId);
                    if (ChatObject.canUserDoAdminAction(fromChat, ChatObject.ACTION_DELETE_MESSAGES)) {
                        deleteReactionRow = rowCount++;
                        divider = true;
                    }
                }
                if (!myProfile && reportReactionMessageId != 0 && !ContactsController.getInstance(currentAccount).isContact(userId)) {
                    reportReactionRow = rowCount++;
                    divider = true;
                }
                if (divider) {
                    reportDividerRow = rowCount++;
                }

                if (hasMedia || (user != null && user.bot && user.bot_can_edit && user.bot_has_main_app) || userInfo != null && userInfo.common_chats_count != 0 || myProfile) {
                    sharedMediaRow = rowCount++;
                } else if (lastSectionRow == -1 && needSendMessage) {
                    sendMessageRow = rowCount++;
                    reportRow = rowCount++;
                    lastSectionRow = rowCount++;
                }
            }
        } else if (isTopic) {
            if (emptyRow < 0 && emptyRow2 < 0) {
                if (hasMusic || peerColor != null || actionsView == null) {
                    emptyRow2 = rowCount++;
                } else {
                    emptyRow = rowCount++;
                }
            }
            usernameRow = rowCount++;
            if (actionsView == null) {
                notificationsSimpleRow = rowCount++;
            }
            if (infoHeaderRowEmpty != -1) {
//                infoEndRowEmpty = rowCount++;
            }
            infoSectionRow = rowCount++;
            if (hasMedia) {
                sharedMediaRow = rowCount++;
            }
        } else if (chatId != 0) {
            if (chatInfo != null && (!TextUtils.isEmpty(chatInfo.about) || chatInfo.location instanceof TLRPC.TL_channelLocation) || ChatObject.isPublic(currentChat)) {
                if (emptyRow < 0 && emptyRow2 < 0) {
                    if (hasMusic || peerColor != null || actionsView == null) {
                        emptyRow2 = rowCount++;
                    } else {
                        emptyRow = rowCount++;
                    }
                }

                if (actionsView == null) {
                    infoHeaderRow = rowCount++;
                }
                if (chatInfo != null) {
                    if (!TextUtils.isEmpty(chatInfo.about)) {
                        channelInfoRow = rowCount++;
                    chatIdRow = rowCount++;
                    ownerRow = rowCount++;
                    }
                    if (chatInfo.location instanceof TLRPC.TL_channelLocation) {
                        locationRow = rowCount++;
                    }
                }
                if (ChatObject.isPublic(currentChat)) {
                    usernameRow = rowCount++;
                }
            }
            if (emptyRow < 0 && emptyRow2 < 0) {
                if (hasMusic || peerColor != null || actionsView == null) {
                    emptyRow2 = rowCount++;
                } else {
                    emptyRow = rowCount++;
                }
            }
            if (actionsView == null) {
                if (infoHeaderRow != -1) {
                    notificationsDividerRow = rowCount++;
                }
                notificationsRow = rowCount++;
            }
            if (rowCount > 0) {
                infoSectionRow = rowCount++;
            }

            if (ChatObject.isChannel(currentChat) && !currentChat.megagroup) {
                if (chatInfo != null && (currentChat.creator || chatInfo.can_view_participants)) {
                    if (actionsView == null) {
                        membersHeaderRow = rowCount++;
                    }
                    subscribersRow = rowCount++;
                    if (chatInfo != null && chatInfo.requests_pending > 0) {
                        subscribersRequestsRow = rowCount++;
                    }
                    administratorsRow = rowCount++;
                    if (chatInfo != null && (chatInfo.banned_count != 0 || chatInfo.kicked_count != 0)) {
                        blockedUsersRow = rowCount++;
                    }
                    if (
                        chatInfo != null && chatInfo.can_view_stars_revenue && (
                            BotStarsController.getInstance(currentAccount).getBotStarsBalance(-chatId).amount > 0 ||
                            BotStarsController.getInstance(currentAccount).hasTransactions(-chatId)
                        ) ||
                        chatInfo != null &&
                        chatInfo.can_view_revenue &&
                        BotStarsController.getInstance(currentAccount).getTONBalance(-chatId) > 0
                    ) {
                        channelBalanceRow = rowCount++;
                    }
                    settingsRow = rowCount++;
                    channelBalanceSectionRow = rowCount++;
                }
            } else {
                if (
                        chatInfo != null &&
                                chatInfo.can_view_stars_revenue && (
                                BotStarsController.getInstance(currentAccount).getBotStarsBalance(-chatId).amount > 0 ||
                                        BotStarsController.getInstance(currentAccount).hasTransactions(-chatId)
                        ) ||
                                chatInfo != null &&
                                        chatInfo.can_view_revenue &&
                                        BotStarsController.getInstance(currentAccount).getTONBalance(-chatId) > 0
                ) {
                    channelBalanceRow = rowCount++;
                    channelBalanceSectionRow = rowCount++;
                }
            }

            if (ChatObject.isChannel(currentChat)) {
                if (!isTopic && chatInfo != null && currentChat.megagroup && chatInfo.participants != null && chatInfo.participants.participants != null && !chatInfo.participants.participants.isEmpty()) {
                    if (!ChatObject.isNotInChat(currentChat) && ChatObject.canAddUsers(currentChat) && chatInfo.participants_count < getMessagesController().maxMegagroupCount) {
                        addMemberRow = rowCount++;
                    }
                    int count = chatInfo.participants.participants.size();
                    if ((count <= 5 || !hasMedia || usersForceShowingIn == 1) && usersForceShowingIn != 2) {
                        if (addMemberRow == -1 && actionsView == null) {
                            membersHeaderRow = rowCount++;
                        }
                        membersStartRow = rowCount;
                        rowCount += count;
                        membersEndRow = rowCount;
                        membersSectionRow = rowCount++;
                        visibleChatParticipants.addAll(chatInfo.participants.participants);
                        if (sortedUsers != null) {
                            visibleSortedUsers.addAll(sortedUsers);
                        }
                        usersForceShowingIn = 1;
                        if (sharedMediaLayout != null) {
                            sharedMediaLayout.setChatUsers(null, null);
                        }
                    } else {
                        if (addMemberRow != -1) {
                            membersSectionRow = rowCount++;
                        }
                        if (sharedMediaLayout != null) {
                            if (!sortedUsers.isEmpty()) {
                                usersForceShowingIn = 2;
                            }
                            sharedMediaLayout.setChatUsers(sortedUsers, chatInfo);
                        }
                    }
                } else {
                    if (!ChatObject.isNotInChat(currentChat) && ChatObject.canAddUsers(currentChat) && chatInfo != null && chatInfo.participants_hidden) {

                        addMemberRow = rowCount++;
                        membersSectionRow = rowCount++;
                    }
                    if (sharedMediaLayout != null) {
                        sharedMediaLayout.updateAdapters();
                    }
                }

                if (lastSectionRow == -1 && currentChat.left && !currentChat.kicked) {
                    long requestedTime = MessagesController.getNotificationsSettings(currentAccount).getLong("dialog_join_requested_time_" + dialogId, -1);
                    if (!(requestedTime > 0 && System.currentTimeMillis() - requestedTime < 1000 * 60 * 2)) {
                        if (actionsView == null || !actionsView.canHaveJoinAction()) {
                            joinRow = rowCount++;
                            lastSectionRow = rowCount++;
                        }
                        hasJoinRow = true;
                    }
                }
            } else if (chatInfo != null) {
                if (!isTopic && chatInfo.participants != null && chatInfo.participants.participants != null && !(chatInfo.participants instanceof TLRPC.TL_chatParticipantsForbidden)) {
                    if (ChatObject.canAddUsers(currentChat) || currentChat.default_banned_rights == null || !currentChat.default_banned_rights.invite_users) {
                        addMemberRow = rowCount++;
                    }
                    int count = chatInfo.participants.participants.size();
                    if (count <= 5 || !hasMedia) {
                        if (addMemberRow == -1 && actionsView == null) {
                            membersHeaderRow = rowCount++;
                        }
                        membersStartRow = rowCount;
                        rowCount += chatInfo.participants.participants.size();
                        membersEndRow = rowCount;
                        membersSectionRow = rowCount++;
                        visibleChatParticipants.addAll(chatInfo.participants.participants);
                        if (sortedUsers != null) {
                            visibleSortedUsers.addAll(sortedUsers);
                        }
                        if (sharedMediaLayout != null) {
                            sharedMediaLayout.setChatUsers(null, null);
                        }
                    } else {
                        if (addMemberRow != -1) {
                            membersSectionRow = rowCount++;
                        }
                        if (sharedMediaLayout != null) {
                            sharedMediaLayout.setChatUsers(sortedUsers, chatInfo);
                        }
                    }
                } else {
                    if (!ChatObject.isNotInChat(currentChat) && ChatObject.canAddUsers(currentChat) && chatInfo.participants_hidden) {
                        addMemberRow = rowCount++;
                        membersSectionRow = rowCount++;
                    }
                    if (sharedMediaLayout != null) {
                        sharedMediaLayout.updateAdapters();
                    }
                }
            }

            if (hasMedia) {
                sharedMediaRow = rowCount++;
            }
        }
        if (sharedMediaRow == -1) {
            bottomPaddingRow = rowCount++;
        }
        final int actionBarHeight = actionBar != null ? ActionBar.getCurrentActionBarHeight() + (actionBar.getOccupyStatusBar() ? AndroidUtilities.statusBarHeight : 0) : 0;
        if (listView == null || prevRowsCount > rowCount || listContentHeight != 0 && listContentHeight + actionBarHeight + getHeaderExtraHeight() < listView.getMeasuredHeight()) {
            lastMeasuredContentWidth = 0;
        }
        if (listView != null) {
            listView.setTranslateSelectorPosition(bizHoursRow);
        }
        if (actionsView != null) {
            actionsView.set(ProfileActionsView.KEY_JOIN, hasJoinRow);
        }
        if (musicView != null) {
            if (userInfo != null) {
                musicView.setMusicDocument(userInfo.saved_music);
            }
            musicView.setVisibility(hasMusic ? View.VISIBLE : View.GONE);
        }
    }

    private Drawable getScamDrawable(int type) {
        if (scamDrawable == null) {
            scamDrawable = new ScamDrawable(11, type);
            scamDrawable.setColor(getThemedColor(Theme.key_avatar_subtitleInProfileBlue));
        }
        return scamDrawable;
    }

    private Drawable getLockIconDrawable() {
        if (lockIconDrawable == null) {
            lockIconDrawable = Theme.chat_lockIconDrawable.getConstantState().newDrawable().mutate();
        }
        return lockIconDrawable;
    }

    private Drawable getVerifiedCrossfadeDrawable(int a) {
        if (verifiedCrossfadeDrawable[a] == null) {
            verifiedDrawable[a] = Theme.profile_verifiedDrawable.getConstantState().newDrawable().mutate();
            verifiedCheckDrawable[a] = Theme.profile_verifiedCheckDrawable.getConstantState().newDrawable().mutate();
            if (a == 1 && peerColor != null) {
                int color = Theme.adaptHSV(peerColor.hasColor6(Theme.isCurrentThemeDark()) ? peerColor.getColor5() : peerColor.getColor3(), +.1f, Theme.isCurrentThemeDark() ? -.1f : -.08f);
                verifiedDrawable[1].setColorFilter(AndroidUtilities.getOffsetColor(color, getThemedColor(Theme.key_player_actionBarTitle), mediaHeaderAnimationProgress, 1.0f), PorterDuff.Mode.MULTIPLY);
                color = Color.WHITE;
                verifiedCheckDrawable[1].setColorFilter(AndroidUtilities.getOffsetColor(color, getThemedColor(Theme.key_windowBackgroundWhite), mediaHeaderAnimationProgress, 1.0f), PorterDuff.Mode.MULTIPLY);
            }
            verifiedCrossfadeDrawable[a] = new CrossfadeDrawable(
                    new CombinedDrawable(verifiedDrawable[a], verifiedCheckDrawable[a]),
                    ContextCompat.getDrawable(getParentActivity(), R.drawable.verified_profile)
            );
        }
        return verifiedCrossfadeDrawable[a];
    }

    private Drawable getPremiumCrossfadeDrawable(int a) {
        if (premiumCrossfadeDrawable[a] == null) {
            premiumStarDrawable[a] = ContextCompat.getDrawable(getParentActivity(), R.drawable.msg_premium_liststar).mutate();
            int color = getThemedColor(Theme.key_profile_verifiedBackground);
            if (a == 1) {
                color = dontApplyPeerColor(color);
            }
            premiumStarDrawable[a].setColorFilter(color, PorterDuff.Mode.MULTIPLY);
            premiumCrossfadeDrawable[a] = new CrossfadeDrawable(premiumStarDrawable[a], ContextCompat.getDrawable(getParentActivity(), R.drawable.msg_premium_prolfilestar).mutate());
        }
        return premiumCrossfadeDrawable[a];
    }

    private Drawable getBotVerificationDrawable(long icon, boolean animated, int a) {
        if (botVerificationDrawable[a] == null) {
            botVerificationDrawable[a] = new AnimatedEmojiDrawable.SwapAnimatedEmojiDrawable(nameTextView[a], AndroidUtilities.dp(17), a == 0 ? AnimatedEmojiDrawable.CACHE_TYPE_EMOJI_STATUS : AnimatedEmojiDrawable.CACHE_TYPE_KEYBOARD);
            botVerificationDrawable[a].offset(0, dp(1));
            if (fragmentViewAttached) {
                botVerificationDrawable[a].attach();
            }
        }
        if (icon != 0) {
            botVerificationDrawable[a].set(icon, animated);
        } else {
            botVerificationDrawable[a].set((Drawable) null, animated);
        }
        updateEmojiStatusDrawableColor();
        return botVerificationDrawable[a];
    }

    private Drawable getEmojiStatusDrawable(TLRPC.EmojiStatus emojiStatus, boolean switchable, boolean animated, int a) {
        if (emojiStatusDrawable[a] == null) {
            emojiStatusDrawable[a] = new AnimatedEmojiDrawable.SwapAnimatedEmojiDrawable(nameTextView[a], AndroidUtilities.dp(24), a == 0 ? AnimatedEmojiDrawable.CACHE_TYPE_EMOJI_STATUS : AnimatedEmojiDrawable.CACHE_TYPE_KEYBOARD);
            if (fragmentViewAttached) {
                emojiStatusDrawable[a].attach();
            }
        }
        if (a == 1) {
            emojiStatusGiftId = null;
        }
        if (emojiStatus instanceof TLRPC.TL_emojiStatus) {
            final TLRPC.TL_emojiStatus status = (TLRPC.TL_emojiStatus) emojiStatus;
            if ((status.flags & 1) == 0 || status.until > (int) (System.currentTimeMillis() / 1000)) {
                emojiStatusDrawable[a].set(status.document_id, animated);
                emojiStatusDrawable[a].setParticles(false, animated);
            } else {
                emojiStatusDrawable[a].set(getPremiumCrossfadeDrawable(a), animated);
                emojiStatusDrawable[a].setParticles(false, animated);
            }
        } else if (emojiStatus instanceof TLRPC.TL_emojiStatusCollectible) {
            final TLRPC.TL_emojiStatusCollectible status = (TLRPC.TL_emojiStatusCollectible) emojiStatus;
            if ((status.flags & 1) == 0 || status.until > (int) (System.currentTimeMillis() / 1000)) {
                if (a == 1) {
                    emojiStatusGiftId = status.collectible_id;
                }
                emojiStatusDrawable[a].set(status.document_id, animated);
                emojiStatusDrawable[a].setParticles(true, animated);
            } else {
                emojiStatusDrawable[a].set(getPremiumCrossfadeDrawable(a), animated);
                emojiStatusDrawable[a].setParticles(false, animated);
            }
        } else {
            emojiStatusDrawable[a].set(getPremiumCrossfadeDrawable(a), animated);
            emojiStatusDrawable[a].setParticles(false, animated);
        }
        updateEmojiStatusDrawableColor();
        return emojiStatusDrawable[a];
    }

    private float lastEmojiStatusProgress;

    private void updateEmojiStatusDrawableColor() {
        updateEmojiStatusDrawableColor(lastEmojiStatusProgress);
    }

    private void updateEmojiStatusDrawableColor(float progress) {
        for (int a = 0; a < 2; ++a) {
            final int fromColor;
            if (peerColor != null && a == 1) {
                fromColor = ColorUtils.blendARGB(peerColor.getStoryColor1(Theme.isCurrentThemeDark()), 0xFFFFFFFF, 0.25f);
            } else {
                fromColor = AndroidUtilities.getOffsetColor(getThemedColor(Theme.key_profile_verifiedBackground), getThemedColor(Theme.key_player_actionBarTitle), mediaHeaderAnimationProgress, 1.0f);
            }
            final int color = ColorUtils.blendARGB(ColorUtils.blendARGB(fromColor, 0xffffffff, progress), getThemedColor(Theme.key_player_actionBarTitle), mediaHeaderAnimationProgress);
            if (emojiStatusDrawable[a] != null) {
                emojiStatusDrawable[a].setColor(color);
            }
            if (botVerificationDrawable[a] != null) {
                botVerificationDrawable[a].setColor(ColorUtils.blendARGB(ColorUtils.blendARGB(fromColor, 0x99ffffff, progress), getThemedColor(Theme.key_player_actionBarTitle), mediaHeaderAnimationProgress));
            }
            if (a == 1) {
                animatedStatusView.setColor(color);
            }
        }
        lastEmojiStatusProgress = progress;
    }

    private void updateEmojiStatusEffectPosition() {
        animatedStatusView.setScaleX(nameTextView[1].getScaleX());
        animatedStatusView.setScaleY(nameTextView[1].getScaleY());
        animatedStatusView.translate(
                nameTextView[1].getX() + nameTextView[1].getRightDrawableX() * nameTextView[1].getScaleX(),
                nameTextView[1].getY() + (nameTextView[1].getHeight() - (nameTextView[1].getHeight() - nameTextView[1].getRightDrawableY()) * nameTextView[1].getScaleY())
        );
    }

    private void updateActionsPosition() {
        if (actionsView == null || onlineTextView[1] == null) {
            return;
        }
        final int newTop = ActionBar.getCurrentActionBarHeight() + (actionBar.getOccupyStatusBar() ? AndroidUtilities.statusBarHeight : 0);
        actionsView.isOpeningLayout = openAnimationInProgress;

        if (openAnimationInProgress && playProfileAnimation == 2 && avatarContainer != null) {
            float vh = avatarContainer.getHeight() * avatarContainer.getScaleY();
            actionsView.clipHeight = avatarContainer.getY() + vh;
            actionsView.setAlpha(avatarAnimationProgress);
            actionsView.updatePosition(listView.getMeasuredWidth(), dp(74));
        } else {
            actionsView.clipHeight = -1;
            float bottom = extraHeight + newTop - dp(hasMusic ? 25 : 0);
            float height = Math.min(dp(74), bottom - newTop);
            actionsView.updatePosition(bottom - height, height);
        }

        if (callToActionItem != null && callToActionItem.getTag() != null) {
            actionsView.isAnimatingCallAction = false;
            if (callToActionItem.getVisibility() == View.VISIBLE) {
                callToActionItem.setVisibility(View.GONE);
            }
            if (callToActionItem.getTag() instanceof ActionBarMenuItem) {
                ((ActionBarMenuItem) callToActionItem.getTag()).setAlpha(1f);
            }
        }
    }

    private void updateMusicPosition() {
        if (musicView == null || onlineTextView[1] == null) {
            return;
        }
        final int newTop = ActionBar.getCurrentActionBarHeight() + (actionBar.getOccupyStatusBar() ? AndroidUtilities.statusBarHeight : 0);
//        musicView.isOpeningLayout = openAnimationInProgress;

        if (openAnimationInProgress && playProfileAnimation == 2 && avatarContainer != null) {
//            float vh = avatarContainer.getHeight() * avatarContainer.getScaleY();
//            musicView.clipHeight = avatarContainer.getY() + vh;
            musicView.setAlpha(avatarAnimationProgress);
            musicView.updatePosition(listView.getMeasuredWidth() + dp(74), getActionsExtraHeight() - dp(74));
        } else {
//            musicView.clipHeight = -1;
            float bottom = extraHeight + newTop + dp(74);
            float height = Math.min(getActionsExtraHeight(), bottom - newTop);
            musicView.updatePosition(bottom - height, height - dp(74));
        }
    }

    private MessagesController.PeerColor peerColor;

    private void updateProfileData(boolean reload) {
        if (avatarContainer == null || nameTextView == null || getParentActivity() == null) {
            return;
        }
        String onlineTextOverride;
        int currentConnectionState = getConnectionsManager().getConnectionState();
        if (currentConnectionState == ConnectionsManager.ConnectionStateWaitingForNetwork) {
            onlineTextOverride = LocaleController.getString(R.string.WaitingForNetwork);
        } else if (currentConnectionState == ConnectionsManager.ConnectionStateConnecting) {
            onlineTextOverride = LocaleController.getString(R.string.Connecting);
        } else if (currentConnectionState == ConnectionsManager.ConnectionStateUpdating) {
            onlineTextOverride = LocaleController.getString(R.string.Updating);
        } else if (currentConnectionState == ConnectionsManager.ConnectionStateConnectingToProxy) {
            onlineTextOverride = LocaleController.getString(R.string.ConnectingToProxy);
        } else {
            onlineTextOverride = null;
        }

        BaseFragment prevFragment = null;
        if (parentLayout != null && parentLayout.getFragmentStack().size() >= 2) {
            BaseFragment fragment = parentLayout.getFragmentStack().get(parentLayout.getFragmentStack().size() - 2);
            if (fragment instanceof ChatActivityInterface) {
                prevFragment = fragment;
            }
            if (fragment instanceof DialogsActivity) {  //
                DialogsActivity dialogsActivity = (DialogsActivity) fragment;
                if (dialogsActivity.rightSlidingDialogContainer != null && dialogsActivity.rightSlidingDialogContainer.currentFragment instanceof ChatActivityInterface) {
                    previousTransitionMainFragment = dialogsActivity;
                    prevFragment = dialogsActivity.rightSlidingDialogContainer.currentFragment;
                }
            }
        }
        final boolean copyFromChatActivity = prevFragment instanceof ChatActivity && ((ChatActivity) prevFragment).avatarContainer != null && ((ChatActivity) prevFragment).getChatMode() == ChatActivity.MODE_SUGGESTIONS;

        TLRPC.TL_forumTopic topic = null;
        boolean shortStatus;

        hasFallbackPhoto = false;
        hasCustomPhoto = false;
        if (userId != 0) {
            TLRPC.User user = getMessagesController().getUser(userId);
            if (user == null) {
                return;
            }
            shortStatus = user.photo != null && user.photo.personal;
            TLRPC.FileLocation photoBig = null;
            if (user.photo != null) {
                photoBig = user.photo.photo_big;
            }
            avatarDrawable.setInfo(currentAccount, user);

            final MessagesController.PeerColor wasPeerColor = peerColor;
            peerColor = MessagesController.PeerColor.fromCollectible(user.emoji_status);
            if (peerColor == null) {
                final int colorId = UserObject.getProfileColorId(user);
                final MessagesController.PeerColors peerColors = MessagesController.getInstance(currentAccount).profilePeerColors;
                peerColor = peerColors == null ? null : peerColors.getColor(colorId);
            }
            if (wasPeerColor != peerColor) {
                updatedPeerColor();
            }
            if (topView != null) {
                topView.setBackgroundEmojiId(UserObject.getProfileEmojiId(user), user != null && user.emoji_status instanceof TLRPC.TL_emojiStatusCollectible, true);
            }
            if (ratingView != null) {
                ratingView.updateColors(peerColor);
            }
            setCollectibleGiftStatus(user.emoji_status instanceof TLRPC.TL_emojiStatusCollectible ? (TLRPC.TL_emojiStatusCollectible) user.emoji_status : null);


            final ImageLocation imageLocation = ImageLocation.getForUserOrChat(user, ImageLocation.TYPE_BIG);
            final ImageLocation thumbLocation = ImageLocation.getForUserOrChat(user, ImageLocation.TYPE_SMALL);
            final ImageLocation videoThumbLocation = ImageLocation.getForUserOrChat(user, ImageLocation.TYPE_VIDEO_BIG);
            VectorAvatarThumbDrawable vectorAvatarThumbDrawable = null;
            TLRPC.VideoSize vectorAvatar = null;
            if (userInfo != null) {
                vectorAvatar = FileLoader.getVectorMarkupVideoSize(user.photo != null && user.photo.personal ? userInfo.personal_photo : userInfo.profile_photo);
                if (vectorAvatar != null) {
                    vectorAvatarThumbDrawable = new VectorAvatarThumbDrawable(vectorAvatar, user.premium, VectorAvatarThumbDrawable.TYPE_PROFILE);
                }
            }
            final ImageLocation videoLocation = avatarsViewPager.getCurrentVideoLocation(thumbLocation, imageLocation);
            if (avatar == null) {
                avatarsViewPager.initIfEmpty(vectorAvatarThumbDrawable, imageLocation, thumbLocation, reload);
            }
            if (avatarBig == null) {
                if (vectorAvatar != null) {
                    avatarImage.setImageDrawable(vectorAvatarThumbDrawable);
                } else if (videoThumbLocation != null && !user.photo.personal) {
                    avatarImage.getImageReceiver().setVideoThumbIsSame(true);
                    avatarImage.setImage(videoThumbLocation, "avatar", imageLocation, "50_50", thumbLocation, "50_50", avatarDrawable, user);
                } else {
                    avatarImage.setImage(videoLocation, ImageLoader.AUTOPLAY_FILTER, imageLocation, "100_100", thumbLocation, "50_50", avatarDrawable, user);
                }
            }

            if (thumbLocation != null && setAvatarRow != -1 || thumbLocation == null && setAvatarRow == -1) {
                updateListAnimated(false);
                needLayout(true);
            }
            if (imageLocation != null && (prevLoadedImageLocation == null || imageLocation.photoId != prevLoadedImageLocation.photoId)) {
                prevLoadedImageLocation = imageLocation;
                getFileLoader().loadFile(imageLocation, user, null, FileLoader.PRIORITY_LOW, 1);
            }

            CharSequence newString = UserObject.getUserName(user);
            String newString2;
            boolean hiddenStatusButton = false;
            if (user.id == getUserConfig().getClientUserId()) {
                if (UserObject.hasFallbackPhoto(getUserInfo())) {
                    newString2 = "";
                    hasFallbackPhoto = true;
                    TLRPC.PhotoSize smallSize = FileLoader.getClosestPhotoSizeWithSize(getUserInfo().fallback_photo.sizes, 1000);
                    if (smallSize != null) {
                        fallbackImage.setImage(ImageLocation.getForPhoto(smallSize, getUserInfo().fallback_photo), "50_50", (Drawable) null, 0, null, UserConfig.getInstance(currentAccount).getCurrentUser(), 0);
                    }
                } else {
                    if (userInfo != null && userInfo.stars_rating != null && userInfo.stars_rating.stars < 0) {
                        newString2 = getString(R.string.StarRatingLevelNegative).toLowerCase(Locale.ROOT);
                    } else {
                        newString2 = LocaleController.getString(R.string.Online);
                    }
                }
            } else if (user.id == UserObject.VERIFY) {
                newString2 = LocaleController.getString(R.string.VerifyCodesNotifications);
            } else if (user.id == 333000 || user.id == 777000 || user.id == 42777) {
                newString2 = LocaleController.getString(R.string.ServiceNotifications);
            } else if (MessagesController.isSupportUser(user)) {
                newString2 = LocaleController.getString(R.string.SupportStatus);
            } else if (isBot) {
                if (user.bot_active_users != 0) {
                    newString2 = LocaleController.formatPluralStringComma("BotUsers", user.bot_active_users, ',');
                } else {
                    newString2 = LocaleController.getString(R.string.Bot);
                }
            } else {
                isOnline[0] = false;
                newString2 = LocaleController.formatUserStatus(currentAccount, user, isOnline, shortStatus ? new boolean[1] : null);
                hiddenStatusButton = user != null && !isOnline[0] && !getUserConfig().isPremium() && user.status != null && (user.status instanceof TLRPC.TL_userStatusRecently || user.status instanceof TLRPC.TL_userStatusLastMonth || user.status instanceof TLRPC.TL_userStatusLastWeek) && user.status.by_me;
                if (onlineTextView[1] != null && !mediaHeaderVisible) {
                    int key = isOnline[0] && peerColor == null ? Theme.key_profile_status : Theme.key_actionBarDefaultSubtitle;
                    onlineTextView[1].setTag(key);
                    if (!isPulledDown) {
                        onlineTextView[1].setTextColor(applyPeerColor(getThemedColor(key), true, isOnline[0]));
                    }
                }
            }
            hasCustomPhoto = user.photo != null && user.photo.personal;
            try {
                newString = Emoji.replaceEmoji(newString, nameTextView[1].getPaint().getFontMetricsInt(), false);
            } catch (Exception ignore) {
            }
            if (copyFromChatActivity) {
                ChatActivity chatActivity = (ChatActivity) prevFragment;
                BackupImageView fromAvatarImage = chatActivity.avatarContainer.getAvatarImageView();
                avatarImage.setAnimateFromImageReceiver(fromAvatarImage.getImageReceiver());
            }
            for (int a = 0; a < 2; a++) {
                if (nameTextView[a] == null) {
                    continue;
                }
                if (a == 0 && copyFromChatActivity) {
                    ChatActivity chatActivity = (ChatActivity) prevFragment;
                    SimpleTextView titleTextView = chatActivity.avatarContainer.getTitleTextView();
                    nameTextView[a].setText(titleTextView.getText());
                    nameTextView[a].setRightDrawable(titleTextView.getRightDrawable());
                    nameTextView[a].setRightDrawable2(titleTextView.getRightDrawable2());
                } else if (a == 0 && user.id != getUserConfig().getClientUserId() && !MessagesController.isSupportUser(user) && user.phone != null && user.phone.length() != 0 && getContactsController().contactsDict.get(user.id) == null &&
                        (getContactsController().contactsDict.size() != 0 || !getContactsController().isLoadingContacts())) {
                    nameTextView[a].setText(PhoneFormat.getInstance().format("+" + user.phone));
                } else {
                    nameTextView[a].setText(newString);
                }
                if (a == 0 && onlineTextOverride != null) {
                    onlineTextView[a].setText(onlineTextOverride);
                } else if (a == 0 && copyFromChatActivity) {
                    ChatActivity chatActivity = (ChatActivity) prevFragment;
                    if (chatActivity.avatarContainer.getSubtitleTextView() instanceof SimpleTextView) {
                        SimpleTextView textView = (SimpleTextView) chatActivity.avatarContainer.getSubtitleTextView();
                        onlineTextView[a].setText(textView.getText());
                    } else if (chatActivity.avatarContainer.getSubtitleTextView() instanceof AnimatedTextView) {
                        AnimatedTextView textView = (AnimatedTextView) chatActivity.avatarContainer.getSubtitleTextView();
                        onlineTextView[a].setText(textView.getText());
                    }
                } else {
                    onlineTextView[a].setText(newString2);
                }
                onlineTextView[a].setDrawablePadding(dp(9));
                onlineTextView[a].setRightDrawableInside(true);
                onlineTextView[a].setRightDrawable(a == 1 && hiddenStatusButton ? getShowStatusButton() : null);
                onlineTextView[a].setRightDrawableOnClick(a == 1 && hiddenStatusButton ? v -> {
                    MessagePrivateSeenView.showSheet(getContext(), currentAccount, getDialogId(), true, null, () -> {
                        getMessagesController().reloadUser(getDialogId());
                    }, resourcesProvider);
                } : null);
                Drawable leftIcon = currentEncryptedChat != null ? getLockIconDrawable() : null;
                boolean rightIconIsPremium = false, rightIconIsStatus = false;
                nameTextView[a].setRightDrawableOutside(a == 0);
                if (a == 0 && !copyFromChatActivity) {
                    if (user.scam || user.fake) {
                        nameTextView[a].setRightDrawable2(getScamDrawable(user.scam ? 0 : 1));
                        nameTextViewRightDrawable2ContentDescription = LocaleController.getString(R.string.ScamMessage);
                    } else if (user.verified) {
                        nameTextView[a].setRightDrawable2(getVerifiedCrossfadeDrawable(a));
                        nameTextViewRightDrawable2ContentDescription = LocaleController.getString(R.string.AccDescrVerified);
                    } else if (getMessagesController().isDialogMuted(dialogId != 0 ? dialogId : userId, topicId)) {
                        nameTextView[a].setRightDrawable2(getThemedDrawable(Theme.key_drawable_muteIconDrawable));
                        nameTextViewRightDrawable2ContentDescription = LocaleController.getString(R.string.NotificationsMuted);
                    } else {
                        nameTextView[a].setRightDrawable2(null);
                        nameTextViewRightDrawable2ContentDescription = null;
                    }
                    if (user != null/* && !getMessagesController().premiumFeaturesBlocked()*/ && !MessagesController.isSupportUser(user) && DialogObject.getEmojiStatusDocumentId(user.emoji_status) != 0) {
                        rightIconIsStatus = true;
                        rightIconIsPremium = false;
                        nameTextView[a].setRightDrawable(getEmojiStatusDrawable(user.emoji_status, false, false, a));
                        nameTextViewRightDrawableContentDescription = LocaleController.getString(R.string.AccDescrPremium);
                    } else if (getMessagesController().isPremiumUser(user)) {
                        rightIconIsStatus = false;
                        rightIconIsPremium = true;
                        nameTextView[a].setRightDrawable(getEmojiStatusDrawable(null, false, false, a));
                        nameTextViewRightDrawableContentDescription = LocaleController.getString(R.string.AccDescrPremium);
                    } else {
                        nameTextView[a].setRightDrawable(null);
                        nameTextViewRightDrawableContentDescription = null;
                    }
                } else if (a == 1) {
                    if (user.scam || user.fake) {
                        nameTextView[a].setRightDrawable2(getScamDrawable(user.scam ? 0 : 1));
                    } else if (user.verified) {
                        nameTextView[a].setRightDrawable2(getVerifiedCrossfadeDrawable(a));
                    } else {
                        nameTextView[a].setRightDrawable2(null);
                    }
                    if (/*!getMessagesController().premiumFeaturesBlocked() && */user != null && !MessagesController.isSupportUser(user) && DialogObject.getEmojiStatusDocumentId(user.emoji_status) != 0) {
                        rightIconIsStatus = true;
                        rightIconIsPremium = false;
                        nameTextView[a].setRightDrawable(getEmojiStatusDrawable(user.emoji_status, true, true, a));
                    } else if (getMessagesController().isPremiumUser(user)) {
                        rightIconIsStatus = false;
                        rightIconIsPremium = true;
                        nameTextView[a].setRightDrawable(getEmojiStatusDrawable(null, true, true, a));
                    } else {
                        nameTextView[a].setRightDrawable(null);
                    }
                }
                if (leftIcon == null && currentEncryptedChat == null && user.bot_verification_icon != 0) {
                    nameTextView[a].setLeftDrawableOutside(true);
                    leftIcon = getBotVerificationDrawable(user.bot_verification_icon, false, a);
                } else {
                    nameTextView[a].setLeftDrawableOutside(false);
                }
                nameTextView[a].setLeftDrawable(leftIcon);
                if (a == 1 && (rightIconIsStatus || rightIconIsPremium)) {
                    nameTextView[a].setRightDrawableOutside(true);
                }
                if (user.self && getMessagesController().isPremiumUser(user)) {
                    nameTextView[a].setRightDrawableOnClick(v -> {
                        showStatusSelect();
                    });
                }
                if (!user.self && getMessagesController().isPremiumUser(user)) {
                    final SimpleTextView textView = nameTextView[a];
                    nameTextView[a].setRightDrawableOnClick(v -> {
                        if (user.emoji_status instanceof TLRPC.TL_emojiStatusCollectible) {
                            TLRPC.TL_emojiStatusCollectible status = (TLRPC.TL_emojiStatusCollectible) user.emoji_status;
                            if (status != null) {
                                Browser.openUrl(getContext(), "https://" + getMessagesController().linkPrefix + "/nft/" + status.slug);
                            }
                            return;
                        }
                        PremiumPreviewBottomSheet premiumPreviewBottomSheet = new PremiumPreviewBottomSheet(ProfileActivity.this, currentAccount, user, resourcesProvider);
                        int[] coords = new int[2];
                        textView.getLocationOnScreen(coords);
                        premiumPreviewBottomSheet.startEnterFromX = textView.rightDrawableX;
                        premiumPreviewBottomSheet.startEnterFromY = textView.rightDrawableY;
                        premiumPreviewBottomSheet.startEnterFromScale = textView.getScaleX();
                        premiumPreviewBottomSheet.startEnterFromX1 = textView.getLeft();
                        premiumPreviewBottomSheet.startEnterFromY1 = textView.getTop();
                        premiumPreviewBottomSheet.startEnterFromView = textView;
                        if (textView.getRightDrawable() == emojiStatusDrawable[1] && emojiStatusDrawable[1] != null && emojiStatusDrawable[1].getDrawable() instanceof AnimatedEmojiDrawable) {
                            premiumPreviewBottomSheet.startEnterFromScale *= 0.98f;
                            TLRPC.Document document = ((AnimatedEmojiDrawable) emojiStatusDrawable[1].getDrawable()).getDocument();
                            if (document != null) {
                                BackupImageView icon = new BackupImageView(getContext());
                                String filter = "160_160";
                                ImageLocation mediaLocation;
                                String mediaFilter;
                                SvgHelper.SvgDrawable thumbDrawable = DocumentObject.getSvgThumb(document.thumbs, Theme.key_windowBackgroundWhiteGrayIcon, 0.2f);
                                TLRPC.PhotoSize thumb = FileLoader.getClosestPhotoSizeWithSize(document.thumbs, 90);
                                if ("video/webm".equals(document.mime_type)) {
                                    mediaLocation = ImageLocation.getForDocument(document);
                                    mediaFilter = filter + "_" + ImageLoader.AUTOPLAY_FILTER;
                                    if (thumbDrawable != null) {
                                        thumbDrawable.overrideWidthAndHeight(512, 512);
                                    }
                                } else {
                                    if (thumbDrawable != null && MessageObject.isAnimatedStickerDocument(document, false)) {
                                        thumbDrawable.overrideWidthAndHeight(512, 512);
                                    }
                                    mediaLocation = ImageLocation.getForDocument(document);
                                    mediaFilter = filter;
                                }
                                icon.setLayerNum(7);
                                icon.setRoundRadius(AndroidUtilities.dp(4));
                                icon.setImage(mediaLocation, mediaFilter, ImageLocation.getForDocument(thumb, document), "140_140", thumbDrawable, document);
                                if (((AnimatedEmojiDrawable) emojiStatusDrawable[1].getDrawable()).canOverrideColor()) {
                                    icon.setColorFilter(new PorterDuffColorFilter(getThemedColor(Theme.key_windowBackgroundWhiteBlueIcon), PorterDuff.Mode.SRC_IN));
                                    premiumPreviewBottomSheet.statusStickerSet = MessageObject.getInputStickerSet(document);
                                } else {
                                    premiumPreviewBottomSheet.statusStickerSet = MessageObject.getInputStickerSet(document);
                                }
                                premiumPreviewBottomSheet.overrideTitleIcon = icon;
                                premiumPreviewBottomSheet.isEmojiStatus = true;
                            }
                        }
                        showDialog(premiumPreviewBottomSheet);
                    });
                }
            }

            if (userId == UserConfig.getInstance(currentAccount).clientUserId) {
                onlineTextView[2].setText(LocaleController.getString(R.string.FallbackTooltip));
                if (userInfo != null && userInfo.stars_rating != null && userInfo.stars_rating.stars < 0) {
                    onlineTextView[3].setText(newString2 = getString(R.string.StarRatingLevelNegative).toLowerCase(Locale.ROOT));
                } else {
                    onlineTextView[3].setText(LocaleController.getString(R.string.Online));
                }
            } else {
                if (user.photo != null && user.photo.personal && user.photo.has_video) {
                    SpannableStringBuilder spannableStringBuilder = new SpannableStringBuilder(newString2);
                    spannableStringBuilder.setSpan(new EmptyStubSpan(), 0, newString2.length(), 0);
                    spannableStringBuilder.append(" d ");
                    spannableStringBuilder.append(LocaleController.getString(R.string.CustomAvatarTooltipVideo));
                    spannableStringBuilder.setSpan(new DotDividerSpan(), newString2.length() + 1, newString2.length() + 2, 0);
                    onlineTextView[2].setText(spannableStringBuilder);
                } else {
                    SpannableStringBuilder spannableStringBuilder = new SpannableStringBuilder(newString2);
                    spannableStringBuilder.setSpan(new EmptyStubSpan(), 0, newString2.length(), 0);
                    spannableStringBuilder.append(" d ");
                    spannableStringBuilder.append(LocaleController.getString(R.string.CustomAvatarTooltip));
                    spannableStringBuilder.setSpan(new DotDividerSpan(), newString2.length() + 1, newString2.length() + 2, 0);
                    onlineTextView[2].setText(spannableStringBuilder);
                }
            }

            onlineTextView[2].setVisibility(View.VISIBLE);
            if (!searchMode) {
                onlineTextView[3].setVisibility(View.VISIBLE);
            }

            if (previousTransitionFragment != null) {
                previousTransitionFragment.checkAndUpdateAvatar();
            }
            avatarImage.getImageReceiver().setVisible(!PhotoViewer.isShowingImage(photoBig) && (getLastStoryViewer() == null || getLastStoryViewer().transitionViewHolder.view != avatarImage), storyView != null);
        } else if (chatId != 0) {
            TLRPC.Chat chat = getMessagesController().getChat(chatId);
            if (chat != null) {
                currentChat = chat;
            } else {
                chat = currentChat;
            }
            if (flagSecure != null) {
                flagSecure.invalidate();
            }

            final MessagesController.PeerColor wasPeerColor = peerColor;
            peerColor = MessagesController.PeerColor.fromCollectible(chat.emoji_status);
            if (peerColor == null) {
                final int colorId = ChatObject.getProfileColorId(chat);
                MessagesController.PeerColors peerColors = MessagesController.getInstance(currentAccount).profilePeerColors;
                peerColor = peerColors == null ? null : peerColors.getColor(colorId);
            }
            if (wasPeerColor != peerColor) {
                updatedPeerColor();
            }
            if (topView != null) {
                topView.setBackgroundEmojiId(ChatObject.getProfileEmojiId(chat), chat != null && chat.emoji_status instanceof TLRPC.TL_emojiStatusCollectible, true);
            }
            setCollectibleGiftStatus(chat.emoji_status instanceof TLRPC.TL_emojiStatusCollectible ? (TLRPC.TL_emojiStatusCollectible) chat.emoji_status : null);

            if (isTopic) {
                topic = getMessagesController().getTopicsController().findTopic(chatId, topicId);
            }

            CharSequence statusString;
            CharSequence profileStatusString;
            boolean profileStatusIsButton = false;
            if (ChatObject.isChannel(chat)) {
                if (!isTopic && (chatInfo == null || !currentChat.megagroup && (chatInfo.participants_count == 0 || ChatObject.hasAdminRights(currentChat) || chatInfo.can_view_participants))) {
                    if (currentChat.megagroup) {
                        statusString = profileStatusString = LocaleController.getString(R.string.Loading).toLowerCase();
                    } else {
                        if (ChatObject.isPublic(chat)) {
                            statusString = profileStatusString = LocaleController.getString(R.string.ChannelPublic).toLowerCase();
                        } else {
                            statusString = profileStatusString = LocaleController.getString(R.string.ChannelPrivate).toLowerCase();
                        }
                    }
                } else {
                    if (isTopic) {
                        int count = 0;
                        if (topic != null) {
                            count = topic.totalMessagesCount - 1;
                        }
                        if (count > 0) {
                            statusString = LocaleController.formatPluralString("messages", count, count);
                        } else {
                            statusString = formatString("TopicProfileStatus", R.string.TopicProfileStatus, chat.title);
                        }
                        SpannableString arrowString = new SpannableString(">");
                        arrowString.setSpan(new ColoredImageSpan(R.drawable.arrow_newchat), 0, 1, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                        profileStatusString = new SpannableStringBuilder(chat.title).append(' ').append(arrowString);
                        profileStatusIsButton = true;
                    } else if (currentChat.megagroup) {
                        if (onlineCount > 1 && chatInfo.participants_count != 0) {
                            statusString = String.format("%s, %s | ID: -%s", LocaleController.formatPluralString("Members", chatInfo.participants_count), LocaleController.formatPluralString("OnlineCount", Math.min(onlineCount, chatInfo.participants_count)), chatId);
                            profileStatusString = String.format("%s, %s", LocaleController.formatPluralStringComma("Members", chatInfo.participants_count), LocaleController.formatPluralStringComma("OnlineCount", Math.min(onlineCount, chatInfo.participants_count)));
                        } else {
                            if (chatInfo.participants_count == 0) {
                                if (chat.has_geo) {
                                    statusString = profileStatusString = LocaleController.getString(R.string.MegaLocation).toLowerCase();
                                } else if (ChatObject.isPublic(chat)) {
                                    statusString = profileStatusString = LocaleController.getString(R.string.MegaPublic).toLowerCase();
                                } else {
                                    statusString = profileStatusString = LocaleController.getString(R.string.MegaPrivate).toLowerCase();
                                }
                            } else {
                                statusString = LocaleController.formatPluralString("Members", chatInfo.participants_count) + " | ID: -" + chatId;
                                profileStatusString = LocaleController.formatPluralStringComma("Members", chatInfo.participants_count);
                            }
                        }
                    } else {
                        int[] result = new int[1];
                        String shortNumber = LocaleController.formatShortNumber(chatInfo.participants_count, result);
                        if (currentChat.megagroup) {
                            statusString = LocaleController.formatPluralString("Members", chatInfo.participants_count);
                            profileStatusString = LocaleController.formatPluralStringComma("Members", chatInfo.participants_count);
                        } else {
                            statusString = LocaleController.formatPluralString("Subscribers", chatInfo.participants_count) + " | ID: -100" + chatId;
                            profileStatusString = LocaleController.formatPluralStringComma("Subscribers", chatInfo.participants_count);
                        }
                    }
                }
            } else {
                if (ChatObject.isKickedFromChat(chat)) {
                    statusString = profileStatusString = LocaleController.getString(R.string.YouWereKicked);
                } else if (ChatObject.isLeftFromChat(chat)) {
                    statusString = profileStatusString = LocaleController.getString(R.string.YouLeft);
                } else {
                    int count = chat.participants_count;
                    if (chatInfo != null && chatInfo.participants != null) {
                        count = chatInfo.participants.participants.size();
                    }
                    if (count != 0 && onlineCount > 1) {
                        statusString = profileStatusString = String.format("%s, %s", LocaleController.formatPluralString("Members", count), LocaleController.formatPluralString("OnlineCount", onlineCount));
                    } else {
                        statusString = profileStatusString = LocaleController.formatPluralString("Members", count);
                    }
                }
            }
            if (copyFromChatActivity) {
                ChatActivity chatActivity = (ChatActivity) prevFragment;
                if (chatActivity.avatarContainer.getSubtitleTextView() instanceof SimpleTextView) {
                    statusString = ((SimpleTextView) chatActivity.avatarContainer.getSubtitleTextView()).getText();
                } else if (chatActivity.avatarContainer.getSubtitleTextView() instanceof AnimatedTextView) {
                    statusString = ((AnimatedTextView) chatActivity.avatarContainer.getSubtitleTextView()).getText();
                }
                BackupImageView fromAvatarImage = chatActivity.avatarContainer.getAvatarImageView();
                avatarImage.setAnimateFromImageReceiver(fromAvatarImage.getImageReceiver());
            }

            boolean changed = false;
            for (int a = 0; a < 2; a++) {
                if (nameTextView[a] == null) {
                    continue;
                }
                if (a == 0 && copyFromChatActivity) {
                    ChatActivity chatActivity = (ChatActivity) prevFragment;
                    SimpleTextView titleTextView = chatActivity.avatarContainer.getTitleTextView();
                    if (nameTextView[a].setText(titleTextView.getText())) {
                        changed = true;
                    }
                    if (nameTextView[a].setRightDrawable(titleTextView.getRightDrawable())) {
                        changed = true;
                    }
                    if (nameTextView[a].setRightDrawable2(titleTextView.getRightDrawable2())) {
                        changed = true;
                    }
                } else if (isTopic) {
                    CharSequence title = topic == null ? "" : topic.title;
                    try {
                        title = Emoji.replaceEmoji(title, nameTextView[a].getPaint().getFontMetricsInt(), false);
                    } catch (Exception ignore) {
                    }
                    if (nameTextView[a].setText(title)) {
                        changed = true;
                    }
                } else if (ChatObject.isMonoForum(chat)) {
                    CharSequence title = getString(R.string.ChatMessageSuggestions);
                    if (nameTextView[a].setText(title)) {
                        changed = true;
                    }
                } else if (chat.title != null) {
                    CharSequence title = chat.title;
                    try {
                        title = Emoji.replaceEmoji(title, nameTextView[a].getPaint().getFontMetricsInt(), false);
                    } catch (Exception ignore) {
                    }
                    if (nameTextView[a].setText(title)) {
                        changed = true;
                    }
                }
                nameTextView[a].setLeftDrawableOutside(false);
                nameTextView[a].setLeftDrawable(null);
                nameTextView[a].setRightDrawableOutside(a == 0);
                nameTextView[a].setRightDrawableOnClick(null);
                if (a != 0) {
                    if (chat.scam || chat.fake) {
                        nameTextView[a].setRightDrawable2(getScamDrawable(chat.scam ? 0 : 1));
                        nameTextViewRightDrawableContentDescription = LocaleController.getString(R.string.ScamMessage);
                    } else if (chat.verified) {
                        nameTextView[a].setRightDrawable2(getVerifiedCrossfadeDrawable(a));
                        nameTextViewRightDrawableContentDescription = LocaleController.getString(R.string.AccDescrVerified);
                    } else {
                        nameTextView[a].setRightDrawable2(null);
                        nameTextViewRightDrawableContentDescription = null;
                    }
                    if (DialogObject.getEmojiStatusDocumentId(chat.emoji_status) != 0) {
                        nameTextView[a].setRightDrawable(getEmojiStatusDrawable(chat.emoji_status, true, false, a));
                        nameTextView[a].setRightDrawableOutside(true);
                        nameTextViewRightDrawableContentDescription = null;
                        if (ChatObject.canChangeChatInfo(chat)) {
                            nameTextView[a].setRightDrawableOnClick(v -> {
                                showStatusSelect();
                            });
                            if (preloadedChannelEmojiStatuses) {
                                preloadedChannelEmojiStatuses = true;
                                getMediaDataController().loadRestrictedStatusEmojis();
                            }
                        } else if (chat.emoji_status instanceof TLRPC.TL_emojiStatusCollectible) {
                            final String slug = ((TLRPC.TL_emojiStatusCollectible) chat.emoji_status).slug;
                            nameTextView[a].setRightDrawableOnClick(v -> {
                                Browser.openUrl(getContext(), "https://" + getMessagesController().linkPrefix + "/nft/" + slug);
                            });
                        }
                    }
                } else if (!copyFromChatActivity) {
                    if (chat.scam || chat.fake) {
                        nameTextView[a].setRightDrawable2(getScamDrawable(chat.scam ? 0 : 1));
                    } else if (chat.verified) {
                        nameTextView[a].setRightDrawable2(getVerifiedCrossfadeDrawable(a));
                    } else if (getMessagesController().isDialogMuted(-chatId, topicId)) {
                        nameTextView[a].setRightDrawable2(getThemedDrawable(Theme.key_drawable_muteIconDrawable));
                    } else {
                        nameTextView[a].setRightDrawable2(null);
                    }
                    if (DialogObject.getEmojiStatusDocumentId(chat.emoji_status) != 0) {
                        nameTextView[a].setRightDrawable(getEmojiStatusDrawable(chat.emoji_status, false, false, a));
                        nameTextView[a].setRightDrawableOutside(true);
                    } else {
                        nameTextView[a].setRightDrawable(null);
                    }
                }
                if (chat.bot_verification_icon != 0) {
                    nameTextView[a].setLeftDrawableOutside(true);
                    nameTextView[a].setLeftDrawable(getBotVerificationDrawable(chat.bot_verification_icon, false, a));
                } else {
                    nameTextView[a].setLeftDrawable(null);
                }
                if (a == 0 && onlineTextOverride != null) {
                    onlineTextView[a].setText(onlineTextOverride);
                } else {
                    if (copyFromChatActivity || (currentChat.megagroup && chatInfo != null && onlineCount > 0) || isTopic) {
                        onlineTextView[a].setText(a == 0 ? statusString : profileStatusString);
                    } else if (a == 0 && ChatObject.isChannel(currentChat) && chatInfo != null && chatInfo.participants_count != 0 && (currentChat.megagroup || currentChat.broadcast)) {
                        int[] result = new int[1];
                        boolean ignoreShort = AndroidUtilities.isAccessibilityScreenReaderEnabled();
                        String shortNumber = ignoreShort ? String.valueOf(result[0] = chatInfo.participants_count) : LocaleController.formatShortNumber(chatInfo.participants_count, result);
                        if (currentChat.megagroup) {
                            if (chatInfo.participants_count == 0) {
                                if (chat.has_geo) {
                                    onlineTextView[a].setText(LocaleController.getString(R.string.MegaLocation).toLowerCase());
                                } else if (ChatObject.isPublic(chat)) {
                                    onlineTextView[a].setText(LocaleController.getString(R.string.MegaPublic).toLowerCase());
                                } else {
                                    onlineTextView[a].setText(LocaleController.getString(R.string.MegaPrivate).toLowerCase());
                                }
                            } else {
                                onlineTextView[a].setText(LocaleController.formatPluralString("Members", result[0]).replace(String.format("%d", result[0]), shortNumber));
                            }
                        } else {
                            onlineTextView[a].setText(LocaleController.formatPluralString("Subscribers", result[0]).replace(String.format("%d", result[0]), shortNumber));
                        }
                    } else {
                        onlineTextView[a].setText(a == 0 ? statusString : profileStatusString);
                    }
                }
                if (a == 1 && isTopic) {
                    if (profileStatusIsButton) {
                        onlineTextView[a].setOnClickListener(e -> goToForum());
                    } else {
                        onlineTextView[a].setOnClickListener(null);
                        onlineTextView[a].setClickable(false);
                    }
                }
            }
            if (changed) {
                needLayout(true);
            }

            TLRPC.FileLocation photoBig = null;
            if (chat.photo != null && !isTopic) {
                photoBig = chat.photo.photo_big;
            }

            final ImageLocation imageLocation;
            final ImageLocation thumbLocation;
            final ImageLocation videoLocation;
            if (isTopic) {
                imageLocation = null;
                thumbLocation = null;
                videoLocation = null;
                ForumUtilities.setTopicIcon(avatarImage, topic, true, true, resourcesProvider);
            } else if (ChatObject.isMonoForum(currentChat)) {
                TLRPC.Chat channel = getMessagesController().getMonoForumLinkedChat(currentChat.id);
                avatarDrawable.setInfo(currentAccount, channel);
                imageLocation = ImageLocation.getForUserOrChat(currentAccount, channel, ImageLocation.TYPE_BIG);
                thumbLocation = ImageLocation.getForUserOrChat(currentAccount, channel, ImageLocation.TYPE_SMALL);
                videoLocation = avatarsViewPager.getCurrentVideoLocation(thumbLocation, imageLocation);
            } else {
                avatarDrawable.setInfo(currentAccount, chat);
                imageLocation = ImageLocation.getForUserOrChat(currentAccount, chat, ImageLocation.TYPE_BIG);
                thumbLocation = ImageLocation.getForUserOrChat(currentAccount, chat, ImageLocation.TYPE_SMALL);
                if (avatarsViewPager != null) {
                    videoLocation = avatarsViewPager.getCurrentVideoLocation(thumbLocation, imageLocation);
                } else {
                    videoLocation = null;
                }
            }

            boolean initied = avatarsViewPager.initIfEmpty(null, imageLocation, thumbLocation, reload);
            if ((imageLocation == null || initied) && isPulledDown) {
                final View view = layoutManager.findViewByPosition(0);
                if (view != null) {
                    listView.smoothScrollBy(0, view.getTop() - getHeaderExtraHeight(), CubicBezierInterpolator.EASE_OUT_QUINT);
                }
            }
            String filter;
            if (videoLocation != null && videoLocation.imageType == FileLoader.IMAGE_TYPE_ANIMATION) {
                filter = ImageLoader.AUTOPLAY_FILTER;
            } else {
                filter = null;
            }
            if (avatarBig == null && !isTopic) {
                avatarImage.setImage(videoLocation, filter, thumbLocation, "50_50", avatarDrawable, chat);
            }
            if (imageLocation != null && (prevLoadedImageLocation == null || imageLocation.photoId != prevLoadedImageLocation.photoId)) {
                prevLoadedImageLocation = imageLocation;
                getFileLoader().loadFile(imageLocation, chat, null, FileLoader.PRIORITY_LOW, 1);
            }
            avatarImage.getImageReceiver().setVisible(!PhotoViewer.isShowingImage(photoBig) && (getLastStoryViewer() == null || getLastStoryViewer().transitionViewHolder.view != avatarImage), storyView != null);
        }

        needLayout(true);
    }

    @Override
    public void clearViews() {
        peerColor = null;
        super.clearViews();
    }

    private void updatedPeerColor() {
        adaptedColors.clear();
        if (topView != null) {
            topView.setBackgroundColorId(peerColor, true);
        }
        if (onlineTextView[1] != null) {
            int statusColor;
            if (onlineTextView[1].getTag() instanceof Integer) {
                statusColor = getThemedColor((Integer) onlineTextView[1].getTag());
            } else {
                statusColor = getThemedColor(Theme.key_actionBarDefaultSubtitle);
            }
            onlineTextView[1].setTextColor(ColorUtils.blendARGB(applyPeerColor(statusColor, true, isOnline[0]), 0xB3FFFFFF, currentExpandAnimatorValue));
        }
        if (showStatusButton != null) {
            showStatusButton.setBackgroundColor(ColorUtils.blendARGB(Theme.multAlpha(Theme.adaptHSV(actionBarBackgroundColor, +0.18f, -0.1f), 0.5f), 0x23ffffff, currentExpandAnimatorValue));
        }
        if (actionBar != null) {
            actionBar.setItemsColor(ColorUtils.blendARGB(peerColor != null ? Color.WHITE : getThemedColor(Theme.key_actionBarDefaultIcon), getThemedColor(Theme.key_actionBarActionModeDefaultIcon), mediaHeaderAnimationProgress), false);
            actionBar.setItemsBackgroundColor(ColorUtils.blendARGB(peerColor != null ? Theme.ACTION_BAR_WHITE_SELECTOR_COLOR : peerColor != null ? 0x20ffffff : getThemedColor(Theme.key_avatar_actionBarSelectorBlue), getThemedColor(Theme.key_actionBarActionModeDefaultSelector), mediaHeaderAnimationProgress), false);
        }
        if (verifiedDrawable[1] != null) {
            final int color1 = peerColor != null ? Theme.adaptHSV(ColorUtils.blendARGB(peerColor.getColor2(), peerColor.hasColor6(Theme.isCurrentThemeDark()) ? peerColor.getColor5() : peerColor.getColor3(), .4f), +.1f, Theme.isCurrentThemeDark() ? -.1f : -.08f) : getThemedColor(Theme.key_profile_verifiedBackground);
            final int color2 = getThemedColor(Theme.key_player_actionBarTitle);
            verifiedDrawable[1].setColorFilter(AndroidUtilities.getOffsetColor(color1, color2, mediaHeaderAnimationProgress, 1.0f), PorterDuff.Mode.MULTIPLY);
        }
        if (verifiedCheckDrawable[1] != null) {
            final int color1 = peerColor != null ? Color.WHITE : dontApplyPeerColor(getThemedColor(Theme.key_profile_verifiedCheck));
            final int color2 = getThemedColor(Theme.key_windowBackgroundWhite);
            verifiedCheckDrawable[1].setColorFilter(AndroidUtilities.getOffsetColor(color1, color2, mediaHeaderAnimationProgress, 1.0f), PorterDuff.Mode.MULTIPLY);
        }
        if (nameTextView[1] != null) {
            nameTextView[1].setTextColor(ColorUtils.blendARGB(ColorUtils.blendARGB(peerColor != null ? Color.WHITE : getThemedColor(Theme.key_profile_title), getThemedColor(Theme.key_player_actionBarTitle), mediaHeaderAnimationProgress), Color.WHITE, currentExpandAnimatorValue));
        }
        if (autoDeletePopupWrapper != null && autoDeletePopupWrapper.textView != null) {
            autoDeletePopupWrapper.textView.invalidate();
        }
        if (lockIconDrawable != null) {
            lockIconDrawable.setColorFilter(peerColor != null ? Color.WHITE : ColorUtils.blendARGB(getThemedColor(Theme.key_chat_lockIcon), Color.WHITE, avatarAnimationProgress), PorterDuff.Mode.MULTIPLY);
        }
        if (musicView != null) {
            musicView.setColor(peerColor);
        }
        AndroidUtilities.forEachViews(listView, view -> {
            if (view instanceof HeaderCell) {
                ((HeaderCell) view).setTextColor(dontApplyPeerColor(getThemedColor(Theme.key_windowBackgroundWhiteBlueHeader), false));
            } else if (view instanceof TextDetailCell) {
                ((TextDetailCell) view).updateColors();
            } else if (view instanceof TextCell) {
                ((TextCell) view).updateColors();
            } else if (view instanceof AboutLinkCell) {
                ((AboutLinkCell) view).updateColors();
            } else if (view instanceof NotificationsCheckCell) {
                ((NotificationsCheckCell) view).getCheckBox().invalidate();
            } else if (view instanceof ProfileHoursCell) {
                ((ProfileHoursCell) view).updateColors();
            } else if (view instanceof ProfileChannelCell) {
                ((ProfileChannelCell) view).updateColors();
            }
            final int viewType = listAdapter.getItemViewType(listView.getChildAdapterPosition(view));
            listAdapter.setBackground(view, viewType);
        });
        if (sharedMediaLayout != null && sharedMediaLayout.scrollSlidingTextTabStrip != null) {
            sharedMediaLayout.scrollSlidingTextTabStrip.updateColors();
        }
        if (sharedMediaLayout != null && sharedMediaLayout.giftsContainer != null) {
            sharedMediaLayout.giftsContainer.updateColors();
        }
        writeButtonSetBackground();
        updateEmojiStatusDrawableColor();
        if (storyView != null) {
            storyView.update();
        }
        if (giftsView != null) {
            giftsView.update();
        }
    }

    private int dontApplyPeerColor(int color) {
        return dontApplyPeerColor(color, true, null);
    }

    private int dontApplyPeerColor(int color, boolean actionBar) {
        return dontApplyPeerColor(color, actionBar, null);
    }

    private final SparseIntArray adaptedColors = new SparseIntArray();

    private int dontApplyPeerColor(int color, boolean actionBar, Boolean online) {
        return color;
    }

    private int applyPeerColor(int color, boolean actionBar, Boolean online) {
        if (!actionBar && isSettings()) return color;
        if (peerColor != null) {
            if (!actionBar) {
                int index = adaptedColors.indexOfKey(color);
                if (index < 0) {
                    final int baseColor = Theme.adaptHSV(peerColor.getBgColor1(Theme.isCurrentThemeDark()), Theme.isCurrentThemeDark() ? 0 : +.05f, Theme.isCurrentThemeDark() ? -.1f : -.04f);
                    int adapted = OKLCH.adapt(color, baseColor);
                    adaptedColors.put(color, adapted);
                    return adapted;
                } else {
                    return adaptedColors.valueAt(index);
                }
            }
            final int baseColor = getThemedColor(actionBar ? Theme.key_actionBarDefault : Theme.key_windowBackgroundWhiteBlueIcon);
            final int storyColor = ColorUtils.blendARGB(peerColor.getStoryColor1(Theme.isCurrentThemeDark()), peerColor.getStoryColor2(Theme.isCurrentThemeDark()), .5f);
            int accentColor = actionBar ? storyColor : peerColor.getBgColor1(Theme.isCurrentThemeDark());
            if (!Theme.hasHue(baseColor)) {
                return online != null && !online ? Theme.adaptHSV(Theme.multAlpha(storyColor, .7f), -.2f, +.2f) : storyColor;
            }
            return Theme.changeColorAccent(baseColor, accentColor, color, Theme.isCurrentThemeDark(), online != null && !online ? Theme.multAlpha(storyColor, .7f) : storyColor);
        }
        return color;
    }

    private int applyPeerColor2(int color) {
        if (peerColor != null) {
            int accentColor = peerColor.getBgColor2(Theme.isCurrentThemeDark());
            return Theme.changeColorAccent(getThemedColor(Theme.key_windowBackgroundWhiteBlueIcon), accentColor, color, Theme.isCurrentThemeDark(), accentColor);
        }
        return color;
    }

    private void createActionBarMenu(boolean animated) {
        if (actionBar == null || otherItem == null) {
            return;
        }
        Context context = actionBar.getContext();
        otherItem.removeAllSubItems();
        animatingItem = null;

        editItemVisible = false;
        callItemVisible = false;
        isCallAvailable = false;
        videoCallItemVisible = false;
        canSearchMembers = false;
        boolean selfUser = false;

        boolean shareAction = false;
        boolean discussAction = false;
        boolean giftAction = false;
        boolean streamAction = false;
        boolean voiceChatAction = false;
        boolean leaveAction = false;
        boolean addStoryAction = false;

        if (userId != 0) {
            TLRPC.User user = getMessagesController().getUser(userId);
            if (user == null) {
                return;
            }
            if (UserObject.isUserSelf(user)) {
                if (myProfile) {
                    if (actionsView != null) {
                        editItemVisible = !actionsView.supportsEditInfo();
                    } else {
                        editItemVisible = true;
                    }
                }
                if (!hasMainTabs) {
                    otherItem.addSubItem(edit_info, R.drawable.msg_edit, LocaleController.getString(R.string.EditInfo));
                    if (imageUpdater != null) {
                        otherItem.addSubItem(add_photo, R.drawable.msg_addphoto, LocaleController.getString(R.string.AddPhoto));
                    }
                }
                editColorItem = otherItem.addSubItem(edit_color, R.drawable.menu_profile_colors, LocaleController.getString(R.string.ProfileColorEdit));
                updateEditColorIcon();
                if (myProfile) {
                    setUsernameItem = otherItem.addSubItem(set_username, R.drawable.menu_username_change, getString(R.string.ProfileUsernameEdit));
                    linkItem = otherItem.addSubItem(copy_link_profile, R.drawable.msg_link2, getString(R.string.ProfileCopyLink));
                    updateItemsUsername();
                }
                selfUser = true;
            } else {
                if (user.bot && user.bot_can_edit) {
                    editItemVisible = true;
                }

                if (userInfo != null && userInfo.phone_calls_available) {
                    callItemVisible = true;
                    videoCallItemVisible = userInfo.video_calls_available;
                }
                if (isBot || getContactsController().contactsDict.get(userId) == null) {
                    if (MessagesController.isSupportUser(user)) {
                        if (userBlocked) {
                            otherItem.addSubItem(block_contact, R.drawable.msg_block, LocaleController.getString(R.string.Unblock));
                        }
                        otherItem.addSubItem(add_shortcut, R.drawable.msg_home, LocaleController.getString(R.string.AddShortcut));
                        otherItem.addSubItem(god_mode, R.drawable.msg_info, "God Mode");
