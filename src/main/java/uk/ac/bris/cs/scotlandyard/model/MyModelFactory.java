package uk.ac.bris.cs.scotlandyard.model;

import com.google.common.collect.ImmutableList;

import javax.annotation.Nonnull;

import com.google.common.collect.ImmutableSet;
import uk.ac.bris.cs.scotlandyard.model.ScotlandYard.Factory;

import java.util.List;

/**
 * cw-model
 * Stage 2: Complete this class
 */
public final class MyModelFactory implements Factory<Model> {
	private final class MyModel implements Model{
		private ImmutableSet<Observer> observers;
		private GameSetup setup;
		private Player mrX;
		private ImmutableList<Player> detectives;
		private Board.GameState gameState;

		private MyModel(
				final GameSetup setup,
				final Player mrX,
				final ImmutableList<Player> detectives){
			this.gameState =  new MyGameStateFactory().build(setup, mrX, detectives);
			this.observers = ImmutableSet.of();
		}

		@Nonnull
		@Override
		public Board getCurrentBoard() {
			return gameState;
		}

		@Override
		public void registerObserver(@Nonnull Observer observer) {
		}

		@Override
		public void unregisterObserver(@Nonnull Observer observer) {
		}

		@Nonnull
		@Override
		public ImmutableSet<Observer> getObservers() {
			return observers;
		}


		@Override
		public void chooseMove(@Nonnull Move move) {
			// TODO Advance the model with move, then notify all observers of what what just happened.
			//  you may want to use getWinner() to determine whether to send out Event.MOVE_MADE or Event.GAME_OVER
			this.gameState = gameState.advance(move);
			if(gameState.getWinner().isEmpty()){

			}

		}
	}
	@Nonnull @Override public Model build(GameSetup setup,
	                                      Player mrX,
	                                      ImmutableList<Player> detectives) {
		return new MyModel(setup, mrX, detectives);
	}


}
