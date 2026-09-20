package com.example.aiassistant

import java.text.Normalizer
import java.util.Locale

object LocalKnowledgeBase {
    data class Entry(val topic:String,val aliases:List<String>,val answer:String,val domain:String)
    private fun e(t:String,a:String,x:String,d:String)=Entry(t,a.split("|"),x,d)

    private val entries=listOf(
        e("Australia capital","capital of australia|australia capital|capitala australiei","The capital of Australia is Canberra.","geography"),
        e("Romania capital","capital of romania|romania capital|capitala romaniei","The capital of Romania is Bucharest.","geography"),
        e("Italy capital","capital of italy|italy capital|capitala italiei","The capital of Italy is Rome.","geography"),
        e("France capital","capital of france|france capital|capitala frantei|capitala franței","The capital of France is Paris.","geography"),
        e("Germany capital","capital of germany|germany capital|capitala germaniei","The capital of Germany is Berlin.","geography"),
        e("Spain capital","capital of spain|spain capital|capitala spaniei","The capital of Spain is Madrid.","geography"),
        e("UK capital","capital of united kingdom|uk capital|capitala regatului unit","The capital of the United Kingdom is London.","geography"),
        e("USA capital","capital of united states|usa capital|us capital","The capital of the United States is Washington, D.C.","geography"),
        e("Canada capital","capital of canada|canada capital|capitala canadei","The capital of Canada is Ottawa.","geography"),
        e("Japan capital","capital of japan|japan capital|capitala japoniei","The capital of Japan is Tokyo.","geography"),
        e("China capital","capital of china|china capital|capitala chinei","The capital of China is Beijing.","geography"),
        e("India capital","capital of india|india capital|capitala indiei","The capital of India is New Delhi.","geography"),
        e("Brazil capital","capital of brazil|brazil capital|capitala braziliei","The capital of Brazil is Brasília.","geography"),
        e("Egypt capital","capital of egypt|egypt capital|capitala egiptului","The capital of Egypt is Cairo.","geography"),
        e("Greece capital","capital of greece|greece capital|capitala greciei","The capital of Greece is Athens.","geography"),
        e("Portugal capital","capital of portugal|portugal capital|capitala portugaliei","The capital of Portugal is Lisbon.","geography"),
        e("Pacific Ocean","pacific ocean|pacific vs atlantic|pacific larger atlantic","The Pacific Ocean is larger than the Atlantic Ocean.","geography"),
        e("Earth","what is earth|ce este pamantul|ce este pământul","Earth is the third planet from the Sun and the only astronomical object currently known to support life.","astronomy"),
        e("Moon","what is the moon|ce este luna","The Moon is Earth's natural satellite.","astronomy"),
        e("Sun","what is the sun|ce este soarele","The Sun is the star at the center of the Solar System.","astronomy"),

        e("Gold symbol","chemical symbol for gold|gold symbol|symbol for gold|simbol chimic aur|simbolul aurului","The chemical symbol for gold is Au.","chemistry"),
        e("Silver symbol","chemical symbol for silver|silver symbol|simbol chimic argint","The chemical symbol for silver is Ag.","chemistry"),
        e("Iron symbol","chemical symbol for iron|iron symbol|simbol chimic fier","The chemical symbol for iron is Fe.","chemistry"),
        e("Copper symbol","chemical symbol for copper|copper symbol|simbol chimic cupru","The chemical symbol for copper is Cu.","chemistry"),
        e("Sodium symbol","chemical symbol for sodium|sodium symbol|simbol chimic sodiu","The chemical symbol for sodium is Na.","chemistry"),
        e("Potassium symbol","chemical symbol for potassium|potassium symbol|simbol chimic potasiu","The chemical symbol for potassium is K.","chemistry"),
        e("Oxygen symbol","chemical symbol for oxygen|oxygen symbol|simbol chimic oxigen","The chemical symbol for oxygen is O.","chemistry"),
        e("Hydrogen symbol","chemical symbol for hydrogen|hydrogen symbol|simbol chimic hidrogen","The chemical symbol for hydrogen is H.","chemistry"),
        e("Carbon symbol","chemical symbol for carbon|carbon symbol|simbol chimic carbon","The chemical symbol for carbon is C.","chemistry"),
        e("Nitrogen symbol","chemical symbol for nitrogen|nitrogen symbol|simbol chimic azot","The chemical symbol for nitrogen is N.","chemistry"),
        e("Water","chemical formula for water|water formula|formula apei","The chemical formula for water is H₂O.","chemistry"),
        e("Carbon dioxide","formula for carbon dioxide|carbon dioxide formula|formula dioxidului de carbon","The chemical formula for carbon dioxide is CO₂.","chemistry"),
        e("Atom","what is an atom|ce este un atom|atom","An atom has a nucleus containing protons and usually neutrons, surrounded by electrons.","chemistry"),
        e("Periodic table","what is the periodic table|ce este tabelul periodic","The periodic table organizes chemical elements by atomic number and recurring chemical properties.","chemistry"),
        e("pH","what is ph|ce este ph","pH is a logarithmic measure related to hydrogen-ion activity; lower values are more acidic and higher values more basic.","chemistry"),

        e("Gravity","what is gravity|ce este gravitatia|ce este gravitația","Gravity is the interaction associated with mass and energy that governs attraction and motion.","physics"),
        e("Speed of light","speed of light|viteza luminii","The speed of light in vacuum is exactly 299,792,458 metres per second.","physics"),
        e("Newton second law","newton second law|second law of motion|a doua lege a lui newton","Newton's second law relates net force, mass and acceleration as F = ma in its classical form.","physics"),
        e("Energy conservation","conservation of energy|conservarea energiei","In an isolated system, total energy is conserved; it can change form but is not created or destroyed.","physics"),
        e("Relativity","what is relativity|ce este relativitatea","Relativity describes relationships among space, time, motion and gravity.","physics"),
        e("Black hole","what is a black hole|ce este o gaura neagra|ce este o gaură neagră","A black hole is a region of spacetime bounded by an event horizon from which signals cannot escape to distant observers.","astronomy"),
        e("Quantum mechanics","what is quantum mechanics|ce este mecanica cuantica|ce este mecanica cuantică","Quantum mechanics is the physical theory used to describe matter and radiation at microscopic scales.","physics"),

        e("Photosynthesis","what is photosynthesis|ce este fotosinteza","Photosynthesis converts light energy into chemical energy; oxygenic photosynthesis uses carbon dioxide and water and releases oxygen.","biology"),
        e("Cell","what is a cell biology|ce este o celula|ce este o celulă","A cell is the basic structural and functional unit of living organisms.","biology"),
        e("DNA","what is dna|ce este adn|ce este dna","DNA stores hereditary genetic information in living organisms and some viruses.","biology"),
        e("RNA","what is rna|ce este arn|ce este rna","RNA is a nucleic acid involved in information transfer, gene regulation and sometimes catalysis.","biology"),
        e("Protein","what is a protein|ce este o proteina|ce este o proteină","Proteins are biological polymers made from amino acids that perform structural, catalytic, transport and signaling functions.","biology"),
        e("Evolution","what is evolution biology|ce este evolutia biologica|ce este evoluția biologică","Biological evolution is change in heritable characteristics of populations across generations.","biology"),
        e("Natural selection","what is natural selection|ce este selectia naturala|ce este selecția naturală","Natural selection is a process in which heritable differences affect reproductive success, changing trait frequencies across generations.","biology"),
        e("Virus","what is a virus|ce este un virus","A virus is an infectious biological entity that replicates using a host cell's molecular machinery.","biology"),

        e("Solar System","what is the solar system|ce este sistemul solar","The Solar System consists of the Sun and objects gravitationally bound to it, including planets, moons, asteroids and comets.","astronomy"),
        e("Milky Way","what is the milky way|ce este calea lactee","The Milky Way is the barred spiral galaxy that contains the Solar System.","astronomy"),
        e("Mars","what is mars|ce este marte","Mars is the fourth planet from the Sun and a terrestrial planet.","astronomy"),
        e("Jupiter","what is jupiter|ce este jupiter","Jupiter is the largest planet in the Solar System and a gas giant.","astronomy"),
        e("Saturn","what is saturn|ce este saturn","Saturn is a gas giant known for its prominent ring system.","astronomy"),
        e("Venus","what is venus|ce este venus","Venus is the second planet from the Sun and has a dense carbon-dioxide-rich atmosphere.","astronomy"),
        e("Mercury","what is mercury planet|ce este mercur planeta","Mercury is the smallest and innermost planet of the Solar System.","astronomy"),
        e("Neptune","what is neptune|ce este neptun","Neptune is the eighth and most distant recognized planet from the Sun.","astronomy"),

        e("CPU","what does cpu stand for|cpu meaning|ce inseamna cpu|ce înseamnă cpu","CPU stands for Central Processing Unit.","computing"),
        e("GPU","what does gpu stand for|gpu meaning|ce inseamna gpu|ce înseamnă gpu","GPU stands for Graphics Processing Unit.","computing"),
        e("RAM","what does ram stand for|ram meaning|ce inseamna ram|ce înseamnă ram","RAM stands for Random Access Memory.","computing"),
        e("ROM","what does rom stand for computing|rom meaning computing","ROM stands for Read-Only Memory.","computing"),
        e("HTTP","what does http stand for|http meaning|ce inseamna http|ce înseamnă http","HTTP stands for Hypertext Transfer Protocol.","computing"),
        e("HTTPS","what does https stand for|https meaning","HTTPS is HTTP protected by a cryptographic transport layer, normally TLS.","computing"),
        e("JSON","what is json|ce este json","JSON is a text-based data interchange format for structured data.","computing"),
        e("Algorithm","what is an algorithm|ce este un algoritm","An algorithm is a finite, defined procedure for solving a problem or performing a computation.","computing"),
        e("Database","what is a database|ce este o baza de date|ce este o bază de date","A database is an organized collection of data managed so it can be stored, queried and updated.","computing"),
        e("Operating system","what is an operating system|ce este un sistem de operare","An operating system manages computer hardware and provides services and interfaces for applications.","computing"),
        e("Android","what is android operating system|ce este android","Android is a mobile operating system and software platform built around the Linux kernel and related components.","computing"),
        e("Git","what is git|ce este git","Git is a distributed version-control system used to track changes to files and coordinate software development.","computing"),
        e("GitHub","what is github|ce este github","GitHub is a platform for hosting and collaborating on software repositories and development workflows.","computing"),
        e("Machine learning","what is machine learning|ce este machine learning|ce este invatarea automata|ce este învățarea automată","Machine learning is a field in which algorithms learn patterns from data to make predictions, decisions or representations.","ai"),
        e("Artificial intelligence","what is artificial intelligence|what is ai|ce este inteligenta artificiala|ce este inteligența artificială","Artificial intelligence is the field of computing concerned with systems that perform tasks associated with perception, reasoning, learning, language or decision-making.","ai"),
        e("Neural network","what is a neural network|ce este o retea neuronala|ce este o rețea neuronală","An artificial neural network is a parameterized computational model whose parameters are adjusted to model patterns in data.","ai"),

        e("Pi","what is pi|valoarea lui pi|pi number","π is the ratio of a circle's circumference to its diameter; approximately 3.141592653589793.","mathematics"),
        e("Prime number","what is a prime number|ce este un numar prim|ce este un număr prim","A prime number is an integer greater than 1 with exactly two positive divisors: 1 and itself.","mathematics"),
        e("Pythagorean theorem","pythagorean theorem|teorema lui pitagora","For a right triangle, a² + b² = c².","mathematics"),
        e("Derivative","what is a derivative calculus|ce este derivata","A derivative measures the instantaneous rate of change of a function with respect to its variable.","mathematics"),
        e("Integral","what is an integral calculus|ce este o integrala|ce este o integrală","An integral represents accumulation and, under appropriate conditions, is related to an antiderivative by the fundamental theorem of calculus.","mathematics"),

        e("Atmosphere","what is earth atmosphere|ce este atmosfera","Earth's atmosphere is the layer of gases surrounding Earth, dominated by nitrogen and oxygen.","earth-science"),
        e("Climate","what is climate|ce este clima","Climate describes long-term statistical patterns of weather and environmental conditions.","earth-science"),
        e("Weather","what is weather|ce este vremea","Weather describes the short-term state of the atmosphere at a place and time.","earth-science"),
        e("Water cycle","water cycle|ciclul apei","The water cycle describes movement of water through evaporation, condensation, precipitation, infiltration and runoff.","earth-science"),

        e("United Nations","what is the united nations|ce este onu|ce este organizatia natiunilor unite|ce este organizația națiunilor unite","The United Nations is an international organization founded in 1945 for international cooperation and collective action.","history"),
        e("World War II","when was world war ii|al doilea razboi mondial|al doilea război mondial","World War II was a global war fought from 1939 to 1945.","history"),
        e("World War I","when was world war i|primul razboi mondial|primul război mondial","World War I was a global conflict fought mainly from 1914 to 1918.","history"),
        e("Democracy","what is democracy|ce este democratia|ce este democrația","Democracy is a system of government in which political authority is ultimately derived from the people.","civics"),
        e("Constitution","what is a constitution|ce este o constitutie|ce este o constituție","A constitution is a fundamental framework defining governmental institutions, powers, procedures and often rights.","civics"),

        e("Noun","what is a noun|ce este un substantiv","A noun typically refers to a person, place, thing, concept or entity.","language"),
        e("Verb","what is a verb|ce este un verb","A verb typically expresses an action, occurrence, state or relation.","language"),
        e("Adjective","what is an adjective|ce este un adjectiv","An adjective typically modifies a noun or noun phrase by describing a property or quality.","language"),
        e("Translation","what is translation|ce este traducerea","Translation expresses the meaning of a source text in another language.","language"),

        e("Inflation","what is inflation|ce este inflatia|ce este inflația","Inflation is a sustained increase in the general price level of goods and services.","economics"),
        e("GDP","what does gdp stand for|what is gdp|ce inseamna pib|ce este pib","GDP stands for Gross Domestic Product and measures the monetary value of final goods and services produced within an economy over a period.","economics"),
        e("Supply and demand","supply and demand|cerere si oferta|cerere și ofertă","Supply and demand is an economic framework relating quantities offered and wanted to prices and other conditions.","economics"),

        e("Scientific method","what is the scientific method|ce este metoda stiintifica|ce este metoda științifică","The scientific method uses observation, measurement, hypotheses, predictions and empirical checks to evaluate explanations.","science"),
        e("Hypothesis","what is a hypothesis|ce este o ipoteza|ce este o ipoteză","A hypothesis is a proposed explanation or prediction that can be tested against evidence.","science"),
        e("Theory","what is a scientific theory|ce este o teorie stiintifica|ce este o teorie științifică","In science, a theory is a well-supported explanatory framework that accounts for observations and makes testable predictions.","science"),
        e("Evidence","what is evidence|ce este o dovada|ce este o dovadă","Evidence is information or observations used to support, challenge or evaluate a claim.","science")
    )


    /*
     * EXPANDED LOCAL KNOWLEDGE CORE
     * Deterministic offline facts. No network required.
     */
    private val expandedEntries = listOf(
        // ---------- GEOGRAPHY ----------
        e("Afghanistan capital","capital of afghanistan|afghanistan capital|capitala afganistanului","The capital of Afghanistan is Kabul.","geography"),
        e("Albania capital","capital of albania|albania capital|capitala albaniеi","The capital of Albania is Tirana.","geography"),
        e("Algeria capital","capital of algeria|algeria capital|capitala algeriei","The capital of Algeria is Algiers.","geography"),
        e("Argentina capital","capital of argentina|argentina capital|capitala argentinei","The capital of Argentina is Buenos Aires.","geography"),
        e("Armenia capital","capital of armenia|armenia capital","The capital of Armenia is Yerevan.","geography"),
        e("Austria capital","capital of austria|austria capital|capitala austriei","The capital of Austria is Vienna.","geography"),
        e("Belgium capital","capital of belgium|belgium capital|capitala belgiei","The capital of Belgium is Brussels.","geography"),
        e("Bulgaria capital","capital of bulgaria|bulgaria capital|capitala bulgariei","The capital of Bulgaria is Sofia.","geography"),
        e("Chile capital","capital of chile|chile capital|capitala statului chile","The capital of Chile is Santiago.","geography"),
        e("Colombia capital","capital of colombia|colombia capital|capitala columbiei","The capital of Colombia is Bogotá.","geography"),
        e("Croatia capital","capital of croatia|croatia capital|capitala croatiei|capitala croației","The capital of Croatia is Zagreb.","geography"),
        e("Cuba capital","capital of cuba|cuba capital","The capital of Cuba is Havana.","geography"),
        e("Czechia capital","capital of czechia|capital of czech republic|czech republic capital","The capital of Czechia is Prague.","geography"),
        e("Denmark capital","capital of denmark|denmark capital|capitala danemarcei","The capital of Denmark is Copenhagen.","geography"),
        e("Finland capital","capital of finland|finland capital|capitala finlandei","The capital of Finland is Helsinki.","geography"),
        e("Hungary capital","capital of hungary|hungary capital|capitala ungariei","The capital of Hungary is Budapest.","geography"),
        e("Iceland capital","capital of iceland|iceland capital|capitala islandei","The capital of Iceland is Reykjavík.","geography"),
        e("Indonesia capital","capital of indonesia|indonesia capital|capitala indoneziei","The capital of Indonesia is Jakarta.","geography"),
        e("Iran capital","capital of iran|iran capital|capitala iranului","The capital of Iran is Tehran.","geography"),
        e("Iraq capital","capital of iraq|iraq capital|capitala irakului","The capital of Iraq is Baghdad.","geography"),
        e("Ireland capital","capital of ireland|ireland capital|capitala irlandei","The capital of Ireland is Dublin.","geography"),
        e("Israel capital","capital of israel|israel capital","Israel's seat of government and proclaimed capital is Jerusalem; its status is internationally contested.","geography"),
        e("Jordan capital","capital of jordan|jordan capital|capitala iordaniei","The capital of Jordan is Amman.","geography"),
        e("Kenya capital","capital of kenya|kenya capital|capitala kenyei","The capital of Kenya is Nairobi.","geography"),
        e("Lebanon capital","capital of lebanon|lebanon capital|capitala libanului","The capital of Lebanon is Beirut.","geography"),
        e("Lithuania capital","capital of lithuania|lithuania capital|capitala lituaniei","The capital of Lithuania is Vilnius.","geography"),
        e("Luxembourg capital","capital of luxembourg|luxembourg capital","The capital of Luxembourg is Luxembourg City.","geography"),
        e("Malaysia capital","capital of malaysia|malaysia capital|capitala malaeziei","The capital of Malaysia is Kuala Lumpur.","geography"),
        e("Mexico capital","capital of mexico|mexico capital|capitala mexicului","The capital of Mexico is Mexico City.","geography"),
        e("Moldova capital","capital of moldova|moldova capital|capitala moldovei","The capital of Moldova is Chișinău.","geography"),
        e("Monaco capital","capital of monaco|monaco capital","The capital of Monaco is Monaco.","geography"),
        e("Mongolia capital","capital of mongolia|mongolia capital|capitala mongoliei","The capital of Mongolia is Ulaanbaatar.","geography"),
        e("Morocco capital","capital of morocco|morocco capital|capitala marocului","The capital of Morocco is Rabat.","geography"),
        e("Netherlands capital","capital of netherlands|netherlands capital|capitala olandei","The capital of the Netherlands is Amsterdam.","geography"),
        e("New Zealand capital","capital of new zealand|new zealand capital|capitala noii zeelande","The capital of New Zealand is Wellington.","geography"),
        e("Norway capital","capital of norway|norway capital|capitala norvegiei","The capital of Norway is Oslo.","geography"),
        e("Poland capital","capital of poland|poland capital|capitala poloniei","The capital of Poland is Warsaw.","geography"),
        e("South Korea capital","capital of south korea|south korea capital|capitala coreei de sud","The capital of South Korea is Seoul.","geography"),
        e("Sweden capital","capital of sweden|sweden capital|capitala suediei","The capital of Sweden is Stockholm.","geography"),
        e("Switzerland capital","capital of switzerland|switzerland capital|capitala elvetiei|capitala elveției","The federal city of Switzerland is Bern.","geography"),
        e("Turkey capital","capital of turkey|turkey capital|capitala turciei","The capital of Türkiye is Ankara.","geography"),
        e("Ukraine capital","capital of ukraine|ukraine capital|capitala ucrainei","The capital of Ukraine is Kyiv.","geography"),
        e("United Arab Emirates capital","capital of uae|uae capital|capitala emiratelor arabe unite","The capital of the United Arab Emirates is Abu Dhabi.","geography"),
        e("Vietnam capital","capital of vietnam|vietnam capital|capitala vietnamului","The capital of Vietnam is Hanoi.","geography"),

        // ---------- CHEMISTRY ----------
        e("Helium symbol","chemical symbol for helium|helium symbol","The chemical symbol for helium is He.","chemistry"),
        e("Lithium symbol","chemical symbol for lithium|lithium symbol","The chemical symbol for lithium is Li.","chemistry"),
        e("Beryllium symbol","chemical symbol for beryllium|beryllium symbol","The chemical symbol for beryllium is Be.","chemistry"),
        e("Boron symbol","chemical symbol for boron|boron symbol","The chemical symbol for boron is B.","chemistry"),
        e("Fluorine symbol","chemical symbol for fluorine|fluorine symbol","The chemical symbol for fluorine is F.","chemistry"),
        e("Neon symbol","chemical symbol for neon|neon symbol","The chemical symbol for neon is Ne.","chemistry"),
        e("Magnesium symbol","chemical symbol for magnesium|magnesium symbol","The chemical symbol for magnesium is Mg.","chemistry"),
        e("Aluminium symbol","chemical symbol for aluminium|aluminum symbol","The chemical symbol for aluminium is Al.","chemistry"),
        e("Silicon symbol","chemical symbol for silicon|silicon symbol","The chemical symbol for silicon is Si.","chemistry"),
        e("Phosphorus symbol","chemical symbol for phosphorus|phosphorus symbol","The chemical symbol for phosphorus is P.","chemistry"),
        e("Sulfur symbol","chemical symbol for sulfur|sulfur symbol","The chemical symbol for sulfur is S.","chemistry"),
        e("Chlorine symbol","chemical symbol for chlorine|chlorine symbol","The chemical symbol for chlorine is Cl.","chemistry"),
        e("Argon symbol","chemical symbol for argon|argon symbol","The chemical symbol for argon is Ar.","chemistry"),
        e("Calcium symbol","chemical symbol for calcium|calcium symbol","The chemical symbol for calcium is Ca.","chemistry"),
        e("Zinc symbol","chemical symbol for zinc|zinc symbol","The chemical symbol for zinc is Zn.","chemistry"),
        e("Bromine symbol","chemical symbol for bromine|bromine symbol","The chemical symbol for bromine is Br.","chemistry"),
        e("Iodine symbol","chemical symbol for iodine|iodine symbol","The chemical symbol for iodine is I.","chemistry"),
        e("Platinum symbol","chemical symbol for platinum|platinum symbol","The chemical symbol for platinum is Pt.","chemistry"),
        e("Mercury symbol","chemical symbol for mercury|mercury chemical symbol","The chemical symbol for mercury is Hg.","chemistry"),
        e("Lead symbol","chemical symbol for lead|lead symbol","The chemical symbol for lead is Pb.","chemistry"),
        e("Uranium symbol","chemical symbol for uranium|uranium symbol","The chemical symbol for uranium is U.","chemistry"),

        // ---------- PHYSICS ----------
        e("Acceleration","what is acceleration|ce este acceleratia|ce este accelerația","Acceleration is the rate of change of velocity with respect to time.","physics"),
        e("Velocity","what is velocity|ce este viteza vectoriala|ce este viteza vectorială","Velocity is the rate of change of position with respect to time and has both magnitude and direction.","physics"),
        e("Force","what is force physics|ce este forta in fizica|ce este forța în fizică","Force is an interaction that can change an object's motion; its SI unit is the newton.","physics"),
        e("Mass","what is mass physics|ce este masa in fizica|ce este masa în fizică","Mass is a measure of an object's inertia and contributes to its gravitational interaction.","physics"),
        e("Momentum","what is momentum physics|ce este impulsul fizic","Linear momentum is the product of mass and velocity.","physics"),
        e("Kinetic energy","what is kinetic energy|ce este energia cinetica|ce este energia cinetică","For a nonrelativistic particle, kinetic energy is 1/2 mv².","physics"),
        e("Potential energy","what is potential energy|ce este energia potentiala|ce este energia potențială","Potential energy is energy associated with position, configuration or interactions within a system.","physics"),
        e("Electric current","what is electric current|ce este curentul electric","Electric current is the rate at which electric charge flows through a surface.","physics"),
        e("Voltage","what is voltage|ce este tensiunea electrica|ce este tensiunea electrică","Voltage is electric potential difference between two points.","physics"),
        e("Resistance","what is electrical resistance|ce este rezistenta electrica|ce este rezistența electrică","Electrical resistance describes opposition to electric current; in an ohmic conductor V = IR.","physics"),
        e("Frequency","what is frequency physics|ce este frecventa|ce este frecvența","Frequency is the number of cycles or repetitions per unit time.","physics"),
        e("Wavelength","what is wavelength|ce este lungimea de unda|ce este lungimea de undă","Wavelength is the spatial period of a wave.","physics"),
        e("Temperature","what is temperature physics|ce este temperatura","Temperature characterizes the thermal state of a system and is related to average microscopic energy.","physics"),

        // ---------- ASTRONOMY ----------
        e("Earth orbit","how long does earth orbit sun|earth orbital period","Earth completes one orbit around the Sun in about 365.25 days.","astronomy"),
        e("Moon orbit","how long does moon orbit earth|moon orbital period","The Moon's sidereal orbital period around Earth is about 27.3 days.","astronomy"),
        e("Mars moons","how many moons does mars have|mars moons","Mars has two known natural satellites, Phobos and Deimos.","astronomy"),
        e("Jupiter moons","how many moons does jupiter have|jupiter moons","Jupiter has many known natural satellites; the exact count can change as new small moons are confirmed.","astronomy"),
        e("Saturn rings","does saturn have rings|saturn rings","Saturn has a prominent ring system made mainly of particles of water ice and rocky material.","astronomy"),
        e("Venus rotation","how long does venus rotate|venus day","Venus rotates very slowly and in the opposite direction to most planets.","astronomy"),
        e("Mars atmosphere","what is mars atmosphere|mars atmosphere","Mars has a thin atmosphere composed mostly of carbon dioxide.","astronomy"),
        e("Asteroid","what is an asteroid|ce este un asteroid","An asteroid is a relatively small rocky or metallic Solar System body, many of which orbit the Sun.","astronomy"),
        e("Comet","what is a comet|ce este o cometa|ce este o cometă","A comet is an icy Solar System body that can develop a coma and tail when heated near the Sun.","astronomy"),
        e("Exoplanet","what is an exoplanet|ce este o exoplaneta|ce este o exoplanetă","An exoplanet is a planet outside our Solar System that orbits another star or, in some cases, a stellar remnant.","astronomy"),
        e("Light year","what is a light year|ce este un an lumina|ce este un an-lumină","A light-year is a unit of distance equal to the distance light travels in vacuum in one Julian year.","astronomy"),
        e("Parsec","what is a parsec|ce este un parsec","A parsec is an astronomical unit of distance defined through stellar parallax and equal to about 3.26 light-years.","astronomy"),

        // ---------- BIOLOGY ----------
        e("Mitochondria","what are mitochondria|ce sunt mitocondriile","Mitochondria are organelles involved in cellular energy metabolism and ATP production.","biology"),
        e("Ribosome","what is a ribosome|ce este ribozomul","A ribosome is a molecular machine that synthesizes proteins from messenger RNA.","biology"),
        e("Chromosome","what is a chromosome|ce este un cromozom","A chromosome is a DNA-containing structure that carries genetic information.","biology"),
        e("Gene","what is a gene|ce este o gena|ce este o genă","A gene is a region of genetic material associated with functional products or regulation.","biology"),
        e("Genome","what is a genome|ce este genomul","A genome is the complete genetic material of an organism or virus.","biology"),
        e("Bacteria","what are bacteria|ce sunt bacteriile","Bacteria are diverse single-celled organisms without a membrane-bound nucleus.","biology"),
        e("Fungi","what are fungi|ce sunt fungii","Fungi are organisms belonging to a distinct biological kingdom and include yeasts, molds and mushrooms.","biology"),
        e("Ecology","what is ecology|ce este ecologia","Ecology studies interactions among organisms and between organisms and their environment.","biology"),
        e("Ecosystem","what is an ecosystem|ce este un ecosistem","An ecosystem includes living organisms and their physical environment interacting as a system.","biology"),
        e("Food chain","what is a food chain|ce este lantul trofic|ce este lanțul trofic","A food chain describes the transfer of energy and matter through feeding relationships.","biology"),

        // ---------- MATHEMATICS ----------
        e("Zero","what is zero mathematics|ce este zero","Zero is the integer representing no quantity and is the additive identity.","mathematics"),
        e("Negative number","what is a negative number|ce este un numar negativ|ce este un număr negativ","A negative number is a real number less than zero.","mathematics"),
        e("Fraction","what is a fraction|ce este o fractie|ce este o fracție","A fraction represents a quotient of integers, with a numerator and nonzero denominator.","mathematics"),
        e("Percentage","what is a percentage|ce este procentul","A percentage is a number expressed as a fraction of 100.","mathematics"),
        e("Mean","what is arithmetic mean|ce este media aritmetica|ce este media aritmetică","The arithmetic mean is the sum of values divided by the number of values.","mathematics"),
        e("Median","what is median statistics|ce este mediana statistica|ce este mediana statistică","The median is the middle value of an ordered dataset, with standard conventions for even-sized datasets.","mathematics"),
        e("Probability","what is probability|ce este probabilitatea","Probability is a mathematical measure of uncertainty about events, conventionally ranging from 0 to 1.","mathematics"),
        e("Set","what is a set mathematics|ce este o multime matematica|ce este o mulțime matematică","A set is a collection of distinct objects considered as a mathematical object.","mathematics"),
        e("Equation","what is an equation|ce este o ecuatie|ce este o ecuație","An equation is a statement that two expressions have equal values.","mathematics"),
        e("Function","what is a function mathematics|ce este o functie|ce este o funcție","A function assigns each allowed input exactly one output.","mathematics"),

        // ---------- COMPUTING ----------
        e("URL","what is a url|ce este un url","A URL is a Uniform Resource Locator identifying the location or access method of a resource.","computing"),
        e("DNS","what is dns|ce este dns","DNS is the Domain Name System, which maps domain names to network information such as IP addresses.","computing"),
        e("IP address","what is an ip address|ce este o adresa ip|ce este o adresă ip","An IP address identifies a network interface or endpoint using an Internet Protocol addressing scheme.","computing"),
        e("IPv4","what is ipv4|ce este ipv4","IPv4 uses 32-bit addresses, conventionally written as four decimal octets.","computing"),
        e("IPv6","what is ipv6|ce este ipv6","IPv6 uses 128-bit addresses and was designed to expand Internet address space.","computing"),
        e("TCP","what is tcp|what does tcp stand for|ce este tcp","TCP stands for Transmission Control Protocol and provides reliable, ordered byte-stream delivery.","computing"),
        e("UDP","what is udp|what does udp stand for|ce este udp","UDP stands for User Datagram Protocol and provides connectionless datagram transport without TCP-style reliability guarantees.","computing"),
        e("API","what is an api|ce este un api","An API is an Application Programming Interface defining how software components can interact.","computing"),
        e("Compiler","what is a compiler|ce este un compilator","A compiler translates source code into another representation, often machine code or an intermediate form.","computing"),
        e("Programming language","what is a programming language|ce este un limbaj de programare","A programming language is a formal language used to express computations and algorithms.","computing"),
        e("Kotlin","what is kotlin programming language|ce este kotlin","Kotlin is a statically typed programming language developed by JetBrains and widely used for Android development.","computing"),
        e("Java","what is java programming language|ce este java","Java is a statically typed programming language and runtime ecosystem used for many kinds of software.","computing"),
        e("Python","what is python programming language|ce este python","Python is a high-level general-purpose programming language emphasizing readability and a broad standard library.","computing"),
        e("Git commit","what is a git commit|ce este un commit git","A Git commit records a snapshot of tracked project changes in a repository history.","computing"),
        e("Git branch","what is a git branch|ce este o ramura git|ce este o ramură git","A Git branch is a movable reference to commits used to develop an independent line of work.","computing"),

        // ---------- AI ----------
        e("Training data","what is training data ai|ce sunt datele de antrenament","Training data is data used to adjust the parameters of a machine-learning model.","ai"),
        e("Inference","what is ai inference|ce este inferenta ai|ce este inferența ai","Inference is the process of using a trained model to produce outputs from inputs.","ai"),
        e("Large language model","what is a large language model|ce este un model lingvistic mare","A large language model is a neural model trained on large text datasets to model and generate language.","ai"),
        e("Token AI","what is a token in ai|ce este un token ai","In language models, a token is a unit of text processed by the model's tokenizer.","ai"),
        e("Prompt","what is a prompt ai|ce este un prompt","A prompt is input provided to an AI system to specify a task, context or desired response.","ai"),
        e("Embedding","what is an embedding ai|ce este un embedding","An embedding is a numerical vector representation designed to capture useful relationships among data items.","ai"),
        e("Classification","what is classification machine learning|ce este clasificarea machine learning","Classification assigns inputs to one or more predefined categories.","ai"),
        e("Regression","what is regression machine learning|ce este regresia machine learning","Regression models a continuous-valued target from input data.","ai"),

        // ---------- EARTH SCIENCE ----------
        e("Tectonic plates","what are tectonic plates|ce sunt placile tectonice|ce sunt plăcile tectonice","Tectonic plates are large pieces of Earth's lithosphere that move relative to one another.","earth-science"),
        e("Earthquake","what is an earthquake|ce este un cutremur","An earthquake is ground motion caused by a sudden release of energy in Earth's crust or mantle.","earth-science"),
        e("Volcano","what is a volcano|ce este un vulcan","A volcano is a geological structure through which magma, gases or volcanic material can reach the surface.","earth-science"),
        e("Tsunami","what is a tsunami|ce este un tsunami","A tsunami is a series of long ocean waves usually generated by displacement of a large volume of water.","earth-science"),
        e("Ocean","what is an ocean|ce este un ocean","An ocean is a major continuous body of salt water covering much of Earth's surface.","earth-science"),
        e("Continent","what is a continent|ce este un continent","A continent is one of Earth's major landmass divisions.","earth-science"),
        e("Evaporation","what is evaporation|ce este evaporarea","Evaporation is the transition of a substance from liquid to gas at a surface.","earth-science"),
        e("Condensation","what is condensation|ce este condensarea","Condensation is the transition of a substance from gas to liquid.","earth-science"),

        // ---------- SCIENCE ----------
        e("Observation","what is scientific observation|ce este observatia stiintifica|ce este observația științifică","Scientific observation is systematic collection of information about phenomena using senses or instruments.","science"),
        e("Experiment","what is an experiment science|ce este un experiment stiintific|ce este un experiment științific","A scientific experiment is a controlled or structured investigation designed to test hypotheses or measure effects.","science"),
        e("Measurement","what is scientific measurement|ce este masurarea stiintifica|ce este măsurarea științifică","Measurement assigns values to properties according to defined procedures and units.","science"),
        e("Uncertainty","what is measurement uncertainty|ce este incertitudinea masurarii|ce este incertitudinea măsurării","Measurement uncertainty describes the range of values reasonably associated with a measured quantity.","science"),
        e("Correlation","what is correlation statistics|ce este corelatia statistica|ce este corelația statistică","Correlation describes statistical association between variables and does not by itself establish causation.","science"),
        e("Causation","what is causation|ce este cauzalitatea","Causation means that a change in one factor contributes to producing a change in another under specified conditions.","science"),

        // ---------- LANGUAGE ----------
        e("Adverb","what is an adverb|ce este un adverb","An adverb commonly modifies a verb, adjective, another adverb or an entire clause.","language"),
        e("Pronoun","what is a pronoun|ce este un pronume","A pronoun is a word or expression that can substitute for or refer to a noun phrase.","language"),
        e("Preposition","what is a preposition|ce este o prepozitie|ce este o prepoziție","A preposition commonly expresses a relation between a following noun phrase and another part of a sentence.","language"),
        e("Sentence","what is a sentence grammar|ce este o propozitie|ce este o propoziție","A sentence is a grammatical unit capable of expressing a complete proposition or speech act depending on context.","language"),
        e("Alphabet","what is an alphabet|ce este un alfabet","An alphabet is a writing system using a set of symbols representing phonemes or related sound units.","language"),
        e("Grammar","what is grammar|ce este gramatica","Grammar is the system of rules and patterns governing the structure of a language.","language"),

        // ---------- ECONOMICS ----------
        e("Market","what is a market economics|ce este o piata economica|ce este o piață economică","A market is an arrangement through which buyers and sellers exchange goods, services or assets.","economics"),
        e("Interest rate","what is an interest rate|ce este rata dobanzii|ce este rata dobânzii","An interest rate is the price or return associated with borrowing or lending money, usually expressed per period.","economics"),
        e("Unemployment","what is unemployment|ce este somajul|ce este șomajul","Unemployment refers to people without work who are available for work and seeking employment under the statistical definition used.","economics"),
        e("Productivity","what is economic productivity|ce este productivitatea","Productivity measures output relative to inputs over a specified period.","economics"),

        // ---------- CIVICS / HISTORY ----------
        e("Parliament","what is parliament|ce este parlamentul","A parliament is a legislative body, often composed of elected or otherwise designated representatives.","civics"),
        e("Election","what is an election|ce este o alegere electorala|ce este o alegere electorală","An election is a formal process through which voters choose among candidates or options for public office or representation.","civics"),
        e("Law","what is law|ce este legea","Law is a system of rules recognized and enforced by an authority or legal system.","civics"),
        e("Human rights","what are human rights|ce sunt drepturile omului","Human rights are rights and freedoms recognized as belonging to human beings under international and domestic legal frameworks.","civics"),
        e("Roman Empire","what was the roman empire|ce a fost imperiul roman","The Roman Empire was a state centered on Rome that developed from the Roman Republic and lasted in the West until the fifth century.","history"),
        e("Ancient Egypt","what was ancient egypt|ce a fost egiptul antic","Ancient Egypt was a civilization centered along the Nile with a history spanning several millennia.","history"),
        e("Renaissance","what was the renaissance|ce a fost renasterea|ce a fost renașterea","The Renaissance was a period of major cultural, artistic and intellectual change in Europe, traditionally associated with the fourteenth through seventeenth centuries.","history"),

        // ---------- SI UNITS ----------
        e("Metre","what is a metre|what is a meter si unit|ce este metrul","The metre is the SI base unit of length.","science"),
        e("Kilogram","what is a kilogram|ce este kilogramul","The kilogram is the SI base unit of mass.","science"),
        e("Second","what is a second si unit|ce este secunda","The second is the SI base unit of time.","science"),
        e("Ampere","what is an ampere|ce este amperul","The ampere is the SI base unit of electric current.","science"),
        e("Kelvin","what is a kelvin|ce este kelvinul","The kelvin is the SI base unit of thermodynamic temperature.","science"),
        e("Mole","what is a mole chemistry|ce este molul","The mole is the SI base unit for amount of substance.","chemistry"),
        e("Candela","what is a candela|ce este candela","The candela is the SI base unit of luminous intensity.","science")
    )

    private val allEntries = entries + expandedEntries

    private val normalized=allEntries.flatMap { it.aliases.map { a -> normalize(a) to it } }

    fun lookup(input:String):Entry? {
        val q=normalize(input)
        normalized.firstOrNull { q.contains(it.first) }?.second?.let { return it }
        val tokens=q.split(' ').filter { it.length>=3 }.toSet()
        return normalized.map { it.second to score(tokens,normalize(it.first)) }
            .filter { it.second>=0.72 }.maxByOrNull { it.second }?.first
    }

    fun canHandle(input:String)=lookup(input)!=null
    fun answer(input:String)=lookup(input)?.answer
    fun domain(input:String)=lookup(input)?.domain
    fun size()=allEntries.size

    private fun score(q:Set<String>,a:String):Double {
        val t=a.split(' ').filter { it.length>=3 }.toSet()
        if(t.isEmpty()) return 0.0
        return q.intersect(t).size.toDouble()/t.size
    }

    private fun normalize(v:String)=Normalizer.normalize(v.lowercase(Locale.ROOT),Normalizer.Form.NFD)
        .replace("\\p{M}+".toRegex(),"").replace(Regex("\\s+")," ").trim()
}
