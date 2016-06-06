import org.apache.poi.xslf.usermodel._
import java.awt.Color
import java.awt.Rectangle
import java.io.{FileInputStream, FileOutputStream, File}
import scala.collection.JavaConversions._
import org.apache.spark.SparkConf
import org.apache.spark.SparkContext
import org.apache.spark.SparkContext._
object BibleToPPT { 
	val BibleTitleFullKoList = List("창세기", "출애굽기", "레위기", "민수기", "신명기", "여호수아", "사사기", "룻기", "사무엘상", "사무엘하", "열왕기상", "열왕기하", "역대상", "역대하", "에스라", "느헤미야", "에스더", "욥기", "시편", "잠언", "전도서", "아가", "이사야", "예레미야", "예레미야 애가", "에스겔", "다니엘", "호세아", "요엘", "아모스", "오바댜", "요나", "미가", "나훔", "하박국", "스바냐", "학개", "스가랴", "말라기", "마태복음", "마가복음", "누가복음", "요한복음", "사도행전", "로마서", "고린도전서", "고린도후서", "갈라디아서", "에베소서", "빌립보서", "골로새서", "데살로니가전서", "데살로니가후서", "디모데전서", "디모데후서", "디도서", "빌레몬서", "히브리서", "야고보서", "베드로전서", "베드로후서", "요한1서", "요한2서", "요한3서", "유다서", "요한계시록")
	val BibleTitleShortcutKoList = List("창","출","레","민","신","수","삿","룻","삼상","삼하","왕상","왕하","대상","대하","스","느","에","욥","시","잠","전","아","사","렘","애","겔","단","호","욜","암","옵","욘","미","나","합","습","학","슥","말","마","막","눅","요","행","롬","고전","고후","갈","엡","빌","골","살전","살후","전","후","딛","몬","히","약","벧전","벧후","요일","요이","요삼","유","계")
	val BibleTitleFullEnList = List("Genesis","Exodus","Leviticus","Numbers","Deuteronomy","Joshua","Judges","Ruth","1Samuel","2Samuel","1Kings","2Kings","1Chronicles","2Chronicles","Ezra","Nehemiah","Esther","Job","Psalms","Proverbs","Ecclesiastes","SongofSongs","Isaiah","Jeremiah","Lamentations","Ezekiel","Daniel","Hosea","Joel","Amos","Obadiah","Jonah","Micah","Nahum","Habakkuk","Zephaniah","Haggai","Zechariah","Malachi","Matthew","Mark","Luke","John","Acts","Romans","1Corinthians","2Corinthians","Galatians","Ephesians","Philippians","Colossians","1Thessalonians","2Thessalonians","1Timothy","2Timothy","Titus","Philemon","Hebrews","James","1Peter","2Peter","1John","2John","3John","Jude","Revelation")
	val BibleTitleShortcutEnList = List("GEN","EXO","LEV","NUM","DEU","JOS","JDG","RTH","SA1","SA2","KI1","KI2","CH1","CH2","EZR","NEH","EST","JOB","PSA","PRO","ECC","SON","ISA","JER","LAM","EZE","DAN","HOS","JOE","AMO","OBA","JON","MIC","NAH","HAB","ZEP","HAG","ZEC","MAL","MAT","MAR","LUK","JOH","ACT","ROM","CO1","CO2","GAL","EPH","PHI","COL","TH1","TH2","TI1","TI2","TIT","PHM","HEB","JAM","PE1","PE2","JO1","JO2","JO3","JUD","REV")
	val KorTitleRegex = """^[ㄱ-ㅎㅏ-ㅣ가-힣]*""".r
	val numRegex = """[\d]*$""".r
	
	def main(args: Array[String]):Unit = {
		/* Path */
		val ResultFilePath = "./results/result.pptx"
		val EasyBiblePath = "./resource/data/easy_bible.txt"
		val pptSourcePath = "./resource/source.pptx"

		/* input */
		//input format "단6", "다니엘6", "단6:1-28", "다니엘6:1-28"
		val input = readLine()
		val inputSplitByColon = input.split(":")
		val inputTitleAndChapter =
		  if ( inputSplitByColon.length > 1 ) inputSplitByColon(0)
		  else input

		val chapterList = numRegex findAllIn inputTitleAndChapter
		val chapter = chapterList.toList.head
		val korTitleList = KorTitleRegex findAllIn inputTitleAndChapter toList
		val korTitle = korTitleList.head
		val indexBibleTitleFullKoList = BibleTitleFullKoList.indexOf(korTitle)
		val korTitleFormatted =
		  if ( indexBibleTitleFullKoList != -1 ) BibleTitleShortcutKoList( indexBibleTitleFullKoList )
		  else korTitle
		val searchText = korTitleFormatted + chapter + ":"

		/* To do Bible Range */

		/* spark */
		val conf = new SparkConf().setMaster("local").setAppName("makePPT")
		val sc = new SparkContext(conf)
		val inputRdd = sc.textFile( EasyBiblePath )
		val rdd = inputRdd.filter(line => line.contains(searchText))
		// println("result",rdd.collect().mkString(","))
		
		val inputStream = new FileInputStream( pptSourcePath )
		val ppt = new XMLSlideShow(inputStream)

		val firstSlide = ppt.getSlides()(0)
		val firstSlideShapes = firstSlide.getShapes()
		firstSlideShapes map { shape =>
			shape match {
				case textShape: XSLFTextShape => 
					val chapterShort = input.substring(0,1)
					val range = input.substring(1)
					val chapterAndRange = textShape.getTextParagraphs().last.getTextRuns()
					chapterAndRange.head.setText(chapterShort)
					chapterAndRange.last.setText(range)
					textShape
			}	
    	}
    	
    	//convert scala list by using scala.collection.JavaConversions._
		val slideMaster = ppt.getSlideMasters()(0)

		//get the blank Layout
		val blankLayout = slideMaster.getLayout(SlideLayout.BLANK)

		val dimension = ppt.getPageSize()
    	
    	rdd.collect() map{ bibleTxt =>
    		val bibleTxtSplitSpaceIndex = bibleTxt.indexOf(" ")
			val bibleTxtHead = bibleTxt.substring(0,bibleTxtSplitSpaceIndex)
			val bibleTxtBody = bibleTxt.substring(bibleTxtSplitSpaceIndex)

			//creating a slide with blank layout
	      	val slide = ppt.createSlide(blankLayout)
	      	val shape = slide.createTextBox()
	      	// initial height of the text box is 100 pt but
	  		val anchor = new Rectangle(0, 0, dimension.width, dimension.height)
	  		shape.setAnchor(anchor);
	      	val p = shape.addNewTextParagraph()

		    val r1 = p.addNewTextRun()
		    r1.setText(bibleTxtHead)
		    r1.setFontColor(Color.red)
		    r1.setFontSize(48)

		    val r2 = p.addNewTextRun()
		    r2.setText(bibleTxtBody)
		    r2.setFontColor(Color.black)
		    r2.setFontSize(48)
		    r2.setBold(true)

    	}
    	
		//creating a file object
      	val file = new File( ResultFilePath )
      	val out = new FileOutputStream(file)
       
      	//saving the changes to a file
      	ppt.write(out)
	    out.close()	 

	}

}
