# pus2022

Proszę pamiętać o skopiowaniu plików konfiguracyjnych /*.example i modyfikacji ich w zależności od żądanej konfuguracji

1. Dokończyć buforowanie wiadomości tzn. kiedy logujemy się do określonego konta, "dostajemy" wszystkie wiadomości młodsze od czasu zapamiętanego w klienckiej bazie danych dla określonego nadawcy (B).
2. Grupy użytkowników (wariant lokalny): w klienckiej bazie danych jest tabela w której definiujemy członkostwo w grupie np. (login TEXT, group TEXT); jeżeli odbiorca wiadomości jest określony jako grupa (przez podanie klucza togroup bądź jest poprzedzony specjalnym znakiem), wiadomość jest dostarczana do wszystkich członków grupy (C)
3. Grupy użytkowników (wariant serwerowy): serwer decyduje o tym, do kogo jest rozsyłana wiadomość czyli serwer ma zapamiętane grupy w swojej bazie danych i obsługuje polecenia służące do ich zarządzania. Grupy mogą być globalne tzn. mogą z nich korzystać wszyscy użytkownicy (B).
4. Rozszerzenie GUI klienta o mechanizm odrębnych rozmów w ramach jednej zakładki (B+).
5. Jest więcej niż jeden serwer. Klient może podłączyć się do dowolnego serwera klastra. Wiadomość (nieobsłużona) jest rozsyłana do innych serwerów klastra (A).
6. W tej chwili wyłączenie i powtórne włączenie serwera powoduje, że zalogowany użytkownik zostaje wylogowany. Można spróbować wysłać jeszcze raz ostatnio użyte i pasujące kredencjały (C).
7. Serwer zapamiętuje identyfikatory sesji klienckiej. Jeżeli zgłasza się ktoś z tym samym identyfikatorem, który ostatnio był zalogowany, automatycznie go loguje (B).
8. Przesyłanie zamiast wiadomości - plików od klienta do klienta (przez serwer). Nie akceptuję rozwiązania zakodowania całego pliku w jsonie z wiadomością (A).
9. Śledzenia aktywności użytkowników - każdy klient zgłasza swoją aktywność do serwera np. z każdą wysłaną wiadomością (trudniej: z każdym naciśniętym przyciskiem). Inny użytkownik może poznać nasz stan jako zalogowany-aktywny, zalogowany-nieaktywny, niezalogowany (B).

Na 5:
Jedno A lub 2xB

Na 4:
Jedno B lub 2xC

Na 3:
Jedno C
