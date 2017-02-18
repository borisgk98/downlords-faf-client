package com.faforever.client.rankedmatch;

import com.faforever.client.config.ClientProperties;
import com.faforever.client.game.Faction;
import com.faforever.client.game.GameService;
import com.faforever.client.i18n.I18n;
import com.faforever.client.leaderboard.LeaderboardEntry;
import com.faforever.client.leaderboard.LeaderboardService;
import com.faforever.client.player.Player;
import com.faforever.client.player.PlayerService;
import com.faforever.client.preferences.ForgedAlliancePrefs;
import com.faforever.client.preferences.Ladder1v1Prefs;
import com.faforever.client.preferences.Preferences;
import com.faforever.client.preferences.PreferencesService;
import com.faforever.client.test.AbstractPlainJavaFxTest;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.control.ToggleButton;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.concurrent.CompletableFuture;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

public class Ladder1V1ControllerTest extends AbstractPlainJavaFxTest {

  private static final String USERNAME = "junit";
  private static final int PLAYER_ID = 123;
  @Mock
  private Ladder1v1Controller instance;
  @Mock
  private GameService gameService;
  @Mock
  private PreferencesService preferencesService;
  @Mock
  private PlayerService playerService;
  @Mock
  private LeaderboardService leaderboardService;
  @Mock
  private I18n i18n;
  @Mock
  private BooleanProperty searching1v1Property;
  @Mock
  private Preferences preferences;
  @Mock
  private Ladder1v1Prefs ladder1V1Prefs;
  @Mock
  private ForgedAlliancePrefs forgedAlliancePrefs;

  private ObjectProperty<Player> currentPlayerProperty;
  private ObservableList<Faction> factionList;

  @Before
  public void setUp() throws Exception {
    instance = new Ladder1v1Controller(gameService, preferencesService, playerService, leaderboardService, i18n,
        new ClientProperties());

    Player player = new Player(USERNAME);
    player.setId(PLAYER_ID);
    currentPlayerProperty = new SimpleObjectProperty<>(player);
    factionList = FXCollections.observableArrayList();
    LeaderboardEntry leaderboardEntry = new LeaderboardEntry();
    leaderboardEntry.setRating(500);
    leaderboardEntry.setWinLossRatio(12.23f);
    leaderboardEntry.setRank(100);
    leaderboardEntry.setGamesPlayed(412);
    leaderboardEntry.setUsername(USERNAME);

    when(leaderboardService.getLadder1v1Stats()).thenReturn(CompletableFuture.completedFuture(new ArrayList<>()));
    when(leaderboardService.getEntryForPlayer(PLAYER_ID)).thenReturn(CompletableFuture.completedFuture(leaderboardEntry));
    when(gameService.searching1v1Property()).thenReturn(searching1v1Property);
    when(preferencesService.getPreferences()).thenReturn(preferences);
    when(preferences.getLadder1v1Prefs()).thenReturn(ladder1V1Prefs);
    when(ladder1V1Prefs.getFactions()).thenReturn(factionList);
    when(preferences.getForgedAlliance()).thenReturn(forgedAlliancePrefs);
    when(playerService.getCurrentPlayer()).thenReturn(currentPlayerProperty.get());
    when(playerService.currentPlayerProperty()).thenReturn(currentPlayerProperty);

    loadFxml("theme/play/ranked_1v1.fxml", clazz -> instance);
  }

  @Test
  public void testPostConstructSelectsPreviousFactions() throws Exception {
    factionList.setAll(Faction.SERAPHIM, Faction.AEON);

    instance.initialize();

    assertThat(instance.aeonButton.isSelected(), is(true));
    assertThat(instance.seraphimButton.isSelected(), is(true));
    assertThat(instance.uefButton.isSelected(), is(false));
    assertThat(instance.cybranButton.isSelected(), is(false));
  }

  @Test
  public void testAllFactionButtonsMapped() throws Exception {
    assertThat(instance.factionsToButtons.keySet(), containsInAnyOrder(Faction.AEON, Faction.CYBRAN, Faction.UEF, Faction.SERAPHIM));
  }

  @Test
  public void testGetRoot() throws Exception {
    assertThat(instance.getRoot(), is(instance.ladder1v1Root));
    assertThat(instance.getRoot().getParent(), is(nullValue()));
  }

  @Test
  public void testOnCancelButtonClicked() throws Exception {
    instance.onCancelButtonClicked();

    verify(gameService).stopSearchLadder1v1();
    assertThat(instance.cancelButton.isVisible(), is(false));
    assertThat(instance.playButton.isVisible(), is(true));
    assertThat(instance.searchProgressIndicator.isVisible(), is(false));
    assertThat(instance.searchingForOpponentLabel.isVisible(), is(false));
    for (ToggleButton button : instance.factionsToButtons.values()) {
      assertThat(button.isDisable(), is(false));
    }
  }

  @Test
  public void testOnPlayButtonClicked() throws Exception {
    when(forgedAlliancePrefs.getPath()).thenReturn(Paths.get("."));

    instance.aeonButton.setSelected(true);
    instance.onFactionButtonClicked();

    instance.onPlayButtonClicked();

    verify(gameService).startSearchLadder1v1(any());
    assertThat(instance.cancelButton.isVisible(), is(true));
    assertThat(instance.playButton.isVisible(), is(false));
    assertThat(instance.searchProgressIndicator.isVisible(), is(true));
    assertThat(instance.searchingForOpponentLabel.isVisible(), is(true));
    for (ToggleButton button : instance.factionsToButtons.values()) {
      assertThat(button.isDisable(), is(true));
    }
  }

  @Test
  public void testOnFactionButtonClicked() throws Exception {
    instance.aeonButton.setSelected(true);
    instance.uefButton.setSelected(false);
    instance.cybranButton.setSelected(true);
    instance.seraphimButton.setSelected(false);

    instance.onFactionButtonClicked();

    verify(preferencesService).storeInBackground();
    assertThat(factionList, containsInAnyOrder(Faction.AEON, Faction.CYBRAN));
  }

  @Test
  public void testUpdateRatingLow() throws Exception {
    testUpdateRating(10, "ranked1v1.ratingHint.low");
  }

  private void testUpdateRating(int leaderboardRatingMean, String key) {
    reset(i18n);

    Player currentPlayer = currentPlayerProperty.get();
    currentPlayer.setLeaderboardRatingDeviation(1);
    currentPlayer.setLeaderboardRatingMean(leaderboardRatingMean);

    instance.onDisplay();

    verify(playerService).getCurrentPlayer();
    verify(i18n).get(key);
  }

  @Test
  public void testUpdateRatingModerate() throws Exception {
    testUpdateRating(150, "ranked1v1.ratingHint.moderate");
  }

  @Test
  public void testUpdateRatingGood() throws Exception {
    testUpdateRating(250, "ranked1v1.ratingHint.good");
  }

  @Test
  public void testUpdateRatingHigh() throws Exception {
    testUpdateRating(350, "ranked1v1.ratingHint.high");
  }

  @Test
  public void testUpdateRatingTop() throws Exception {
    testUpdateRating(450, "ranked1v1.ratingHint.top");
  }

  @Test
  public void testUpdateRatingInProgress() throws Exception {
    Player currentPlayer = currentPlayerProperty.get();
    currentPlayer.setLeaderboardRatingDeviation(45);
    currentPlayer.setLeaderboardRatingMean(100);

    instance.onDisplay();

    assertThat(instance.ratingProgressIndicator.isVisible(), is(true));
    assertThat(instance.ratingProgressIndicator.getProgress(), is(5d / 10d));
  }

  @Test
  public void testOnDisplayFiresOnlyOnce() throws Exception {
    verifyZeroInteractions(playerService);
    instance.onDisplay();
    verify(playerService).currentPlayerProperty();
    verify(playerService).getCurrentPlayer();
    verify(playerService).currentPlayerProperty();
    instance.onDisplay();
    verifyNoMoreInteractions(playerService);
  }
}
