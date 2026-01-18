import java.util.*;

public class TerminalBlackjack {
    static class Card {
        final String rank;
        final String suit;
        Card(String r, String s) { rank = r; suit = s; }
        int value() {
            return switch (rank) {
                case "A" -> 11;
                case "K", "Q", "J" -> 10;
                default -> Integer.parseInt(rank);
            };
        }
        public String toString() { return rank + suit; }
    }

    static class Deck {
        final List<Card> cards = new ArrayList<>();
        int idx = 0;
        Deck(Random rand) {
            String[] suits = {"♠","♥","♦","♣"};
            String[] ranks = {"A","2","3","4","5","6","7","8","9","10","J","Q","K"};
            for (String s : suits) for (String r : ranks) cards.add(new Card(r, s));
            Collections.shuffle(cards, rand);
        }
        Card draw() {
            if (idx >= cards.size()) throw new RuntimeException("Deck empty");
            return cards.get(idx++);
        }
    }

    static int bestTotal(List<Card> hand) {
        int total = 0;
        int aces = 0;
        for (Card c : hand) {
            total += c.value();
            if (c.rank.equals("A")) aces++;
        }
        while (total > 21 && aces > 0) {
            total -= 10;
            aces--;
        }
        return total;
    }

    static boolean isBlackjack(List<Card> hand) {
        return hand.size() == 2 && bestTotal(hand) == 21;
    }

    static String handString(List<Card> hand, boolean hideSecond) {
        if (!hideSecond) return hand.toString();
        if (hand.size() < 2) return hand.toString();
        return "[" + hand.get(0) + ", ??]";
    }

    static int readInt(Scanner sc, String prompt) {
        while (true) {
            System.out.print(prompt);
            String line = sc.nextLine().trim();
            try {
                return Integer.parseInt(line);
            } catch (Exception e) {
                System.out.println("Enter a number.");
            }
        }
    }

    static String readCmd(Scanner sc, String prompt) {
        System.out.print(prompt);
        return sc.nextLine().trim().toLowerCase(Locale.ROOT);
    }

    static void clear() {
        System.out.print("\033[H\033[2J");
        System.out.flush();
    }

    public static void main(String[] args) {
        Scanner sc = new Scanner(System.in);
        Random rand = new Random();

        int chips = 100;

        while (true) {
            clear();
            System.out.println("BLACKJACK  |  Chips: " + chips);
            if (chips <= 0) {
                System.out.println("You're out of chips.");
                System.out.print("Restart with 100? (y/n): ");
                String a = sc.nextLine().trim().toLowerCase(Locale.ROOT);
                if (a.equals("y")) { chips = 100; continue; }
                break;
            }

            int bet;
            while (true) {
                bet = readInt(sc, "Bet (1-" + chips + ", 0 to quit): ");
                if (bet == 0) { sc.close(); return; }
                if (bet > 0 && bet <= chips) break;
                System.out.println("Invalid bet.");
            }

            Deck deck = new Deck(rand);
            List<Card> player = new ArrayList<>();
            List<Card> dealer = new ArrayList<>();
            player.add(deck.draw());
            dealer.add(deck.draw());
            player.add(deck.draw());
            dealer.add(deck.draw());

            boolean playerBJ = isBlackjack(player);
            boolean dealerBJ = isBlackjack(dealer);

            int roundDelta = 0;

            if (playerBJ || dealerBJ) {
                clear();
                System.out.println("Dealer: " + handString(dealer, false) + "  (" + bestTotal(dealer) + ")");
                System.out.println("You:    " + handString(player, false) + "  (" + bestTotal(player) + ")");
                if (playerBJ && dealerBJ) {
                    System.out.println("Push (both blackjack).");
                    roundDelta = 0;
                } else if (playerBJ) {
                    int win = (bet * 3) / 2;
                    System.out.println("Blackjack! You win +" + win);
                    roundDelta = win;
                } else {
                    System.out.println("Dealer blackjack. You lose -" + bet);
                    roundDelta = -bet;
                }
                chips += roundDelta;
                System.out.print("(Enter) ");
                sc.nextLine();
                continue;
            }

            boolean playerBust = false;
            boolean doubled = false;

            while (true) {
                clear();
                System.out.println("Dealer: " + handString(dealer, true));
                System.out.println("You:    " + handString(player, false) + "  (" + bestTotal(player) + ")");
                System.out.println("Bet: " + bet + (doubled ? " (doubled)" : ""));
                String cmd = readCmd(sc, "Command [h=hit, s=stand, d=double, q=quit]: ");

                if (cmd.equals("q")) { sc.close(); return; }

                if (cmd.equals("d")) {
                    if (doubled) {
                        System.out.println("Already doubled.");
                        System.out.print("(Enter) ");
                        sc.nextLine();
                        continue;
                    }
                    if (chips < bet) {
                        System.out.println("Not enough chips to double.");
                        System.out.print("(Enter) ");
                        sc.nextLine();
                        continue;
                    }
                    chips -= bet;
                    bet *= 2;
                    doubled = true;
                    player.add(deck.draw());
                    if (bestTotal(player) > 21) playerBust = true;
                    break;
                }

                if (cmd.equals("h")) {
                    player.add(deck.draw());
                    if (bestTotal(player) > 21) {
                        playerBust = true;
                        break;
                    }
                    continue;
                }

                if (cmd.equals("s")) break;

                System.out.println("Invalid command.");
                System.out.print("(Enter) ");
                sc.nextLine();
            }

            if (playerBust) {
                clear();
                System.out.println("Dealer: " + handString(dealer, false) + "  (" + bestTotal(dealer) + ")");
                System.out.println("You:    " + handString(player, false) + "  (" + bestTotal(player) + ")");
                System.out.println("Bust. You lose -" + bet);
                chips -= bet;
                System.out.print("(Enter) ");
                sc.nextLine();
                continue;
            }

            while (bestTotal(dealer) < 17) dealer.add(deck.draw());

            int p = bestTotal(player);
            int d = bestTotal(dealer);

            clear();
            System.out.println("Dealer: " + handString(dealer, false) + "  (" + d + ")");
            System.out.println("You:    " + handString(player, false) + "  (" + p + ")");

            if (d > 21) {
                System.out.println("Dealer busts. You win +" + bet);
                chips += bet;
            } else if (p > d) {
                System.out.println("You win +" + bet);
                chips += bet;
            } else if (p < d) {
                System.out.println("You lose -" + bet);
                chips -= bet;
            } else {
                System.out.println("Push.");
            }

            System.out.print("(Enter) ");
            sc.nextLine();
        }

        sc.close();
    }
}
