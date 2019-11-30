(ns com.left-over.ui.views.about
  (:require [com.left-over.ui.services.navigation :as nav]))

(def ^:private members
  [{:name  "Scott"
    :image "scott.jpg"
    :bio   "Originally from Bergen County, NJ, Scott has been performing in classic rock bands
            since 1992, notably as front man for Baltimore band Til September from 2008-2018.
            He has also played piano since the age of six and performed in musicals, including
            the leading role of Pippin in a community theater production. As an Army Lieutenant
            Colonel, veterinarian, and PhD pharmacologist, Scott has served on active duty since
            2002 in the wake of 9/11, including Operation Iraqi Freedom in 2007. He was the
            Military Idol winner for Ft. Myer in 2005 and was among the finalists in the national
            competition. He has also performed the Star Spangled Banner on a regular basis for the
            Freedom Alliance, as well as military, community, and sporting events. In his infinite
            spare time, he co-owns Neighborhood Veterinary Associates of Clarksburg and studies
            and teaches taekwondo, along with his son, as third degree black belts."}
   {:name  "Jack"
    :image "jack.jpg"
    :bio   "As soon as he learned the first three chords on guitar as a teenager in Baltimore,
            Jack started writing his own material, which now amasses a staggering >200 songs.
            Over the past four decades, Jack has played in several bands, but is best known as
            the founding and enduring of the infamous Phoenix Rising. Always a self-made man,
            Jack co-owns a kitchen and bathroom construction and remodeling business. His
            salt-of-the-earth, friend-til-the-end persona and his strong moral stance on personal
            accountability resonate throughout Left-Over's music and lyrics."}
   {:name  "Brian"
    :image "brian.jpg"
    :bio   "A native of Parkville, MD, Brian picked up percussion at the age of nine and was
            fortunate enough to have his Uncle Bobby, an accomplished jazz and swing drummer, as a
            teacher. This diverse musical palette was refined over the years to form what is now
            the unique backbone of Left-Over. Brian met Jack in 2002, around the time of the
            inception of Phoenix Rising, for which he filled in on drums on occasion. Although the
            original project didn't get off the ground 15 years ago, the timing was right in 2016
            with the current lineup. Brian is a certified Journeyman Machinist in the State of
            Maryland for almost 40 years. He has been married for 30 years and has two sons.
            \"God, Family, Country, and The Major.\""}
   {:name  "Donny"
    :image "donny.jpg"
    :bio   "Born and raised in Baltimore City, music has always been a big part of Donnie's life.
            As a teenager in a 900sf row home with four other family members, the music was
            almost literally \"beaten\" into him, listening to his uncle play drums in the
            basement on a daily basis. He joined his first band, The Muck Rakers, at 15, and has
            been performing ever since with bands such as Charlie Don't Surf, Caffeine, The
            Fashionably Late, and Mars Behind Venus. Since traveling the world during his four
            years in the US Navy, including Desert Shield, Donnie has been in the real estate
            and mortgage business and a father of two."}
   {:name  "Johnny"
    :image "johnny.jpg"
    :bio   "Born on the shores of the Chesapeake Bay in Annapolis Maryland, Johnny has had Old
            Bay and music in his blood for all his life. Picking up the guitar at age five, and
            later tuba, trombone, baritone, as well as bass guitar, vocals, and piano, he went
            on to join Old Mill High School's Marching Patriots, who became Tri-State Champions
            for three years in a row, as well as the Jazz Ensemble, Pit Orchestra, and Concert
            Band. He won \"Best Jazz Guitarist\" in a competition in Nashville, TN, in 1992.
            From there, he went on to sing and play guitar for various original music acts such
            as Echo-7, …soIhadto…, Downpour, and Vote Quimby: Annapolis's Loudest Band.
            Currently, Johnny can be found being a father, a husband, running his music store,
            teaching guitar, and producing bands in his recording studio Inner Sound in
            Nottingham, MD!"}])

(defn root [_]
  [:div
   [:ul.bios
    (for [{:keys [bio name image]} members]
      ^{:key name}
      [:li.bio.box
       [:div.img-container
        [:img {:src (nav/api-for :api/image {:route-params {:image image}})}]]
       [:p bio]])]])
