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

    private val normalized=entries.flatMap { it.aliases.map { a -> normalize(a) to it } }

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
    fun size()=entries.size

    private fun score(q:Set<String>,a:String):Double {
        val t=a.split(' ').filter { it.length>=3 }.toSet()
        if(t.isEmpty()) return 0.0
        return q.intersect(t).size.toDouble()/t.size
    }

    private fun normalize(v:String)=Normalizer.normalize(v.lowercase(Locale.ROOT),Normalizer.Form.NFD)
        .replace("\\p{M}+".toRegex(),"").replace(Regex("\\s+")," ").trim()
}
