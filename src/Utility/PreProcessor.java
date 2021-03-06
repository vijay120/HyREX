package Utility;

import java.util.ArrayList;

import Structures.*;

public class PreProcessor {

	/**
	 * Remove the words that are in the same NP of an entity.
	 * Note: This particular pre-processing didn't improve results.
	 * 
	 * @param bioRelExInpFile
	 * @param psgParsedFileName
	 * @throws Exception
	 */
	public void removeOtherWordsInLeastNPofEnt( String bioRelExInpFile, String psgParsedFileName ) throws Exception {
		
		ArrayList<ArrayList<String>> allSen = FileUtility.readAllMultiLineInputs(bioRelExInpFile);
		
		ArrayList<CFGParseOfSen> listCFGParseOfAllSen = CFGParseOfSen.readCFGParseForAllSen(psgParsedFileName);
		FileUtility.writeInFile(bioRelExInpFile, "", false);
		String psg = "", prevId = ""; 
		
		for ( int s=0; s<allSen.size(); s=s+2 ){
			StringBuilder sb = new StringBuilder();
			
			if ( !prevId.equals(allSen.get(s).get(0)) )
				psg = CFGParseOfSen.getBySenId(listCFGParseOfAllSen,  allSen.get(s).get(0)).psgParse;
			
			prevId = allSen.get(s).get(0);
			
			// find the least phrasal category for each token 			
			String[] tmp = psg.replaceAll("\\)\\)", ") ()").replaceAll("\\s+", " ").trim().split("\\(");
			ArrayList<String[]> listTokPhraseCat = new ArrayList<String[]>();
			int phraseCatIndex = 0, rBrack = 0;
			
			// 1st element in tmp is empty
			for ( int i=1; i<tmp.length; i++ ) {
			
				String[] str = tmp[i].trim().split("\\s+");				
				rBrack = 0;
				
				if ( str.length > 1 ){
					for ( int k=listTokPhraseCat.size()-1; k>=0; k-- ) {
						if ( listTokPhraseCat.get(k).length == 1 && !listTokPhraseCat.get(k)[0].equals(")") && rBrack == 0 ) {
							//str[0] = str[1];
							str[1] = listTokPhraseCat.get(k)[0];
							listTokPhraseCat.add(str);
							break;
						}
						else if ( listTokPhraseCat.get(k).length == 1 && listTokPhraseCat.get(k)[0].equals(")") )
							rBrack++;
						else if ( listTokPhraseCat.get(k).length == 1 && !listTokPhraseCat.get(k)[0].equals(")") && rBrack != 0 )
							rBrack--;								
					}
				}
				else if ( str.length == 1 && !str[0].equals(")") ) {
					str[0] = str[0] + "-" + phraseCatIndex;
					listTokPhraseCat.add(str);
					phraseCatIndex++;
				} else if ( str.length == 1 && str[0].equals(")") )
					listTokPhraseCat.add(str);
			}
			
			// remove all elements other than words
			for ( int i=listTokPhraseCat.size()-1; i>=0; i-- ) {
				if ( listTokPhraseCat.get(i).length == 1 ) {
					listTokPhraseCat.remove(i);
				}
				
			}
			
			// remove the words which are in the least NP of a protein
			// the 1st item in the list is sentence id
			// search from the 2nd token = 3rd item in the list
			for ( int i=2; i<allSen.get(s).size(); i++ ) {
				tmp = allSen.get(s).get(i).split("\\s+"); 
				
				if ( !tmp[3].equals("O") && listTokPhraseCat.get(i-1)[1].contains("NP-")  ) {
					// checking previous token
					if ( i > 1 && !(allSen.get(s).get(i-1).contains("\tB-e") || allSen.get(s).get(i-1).contains("\tI-e")) 
							&& listTokPhraseCat.get(i-1)[1].equals(listTokPhraseCat.get(i-1-1)[1]) 
							//&& !listTokPhraseCat.get(i-1-1)[0].equals("DT") 
							) {
						listTokPhraseCat.remove(i-1-1);
						allSen.get(s).remove(i-1);
						i--;
					}
					/*/ checking next token
					else if ( i < allSen.get(s).size() && listTokPhraseCat.get(i-1)[1].equals(listTokPhraseCat.get(i)[1]) ) {
						listTokPhraseCat.remove(i);
						allSen.remove(s).get(i);
						i--;
					}*/
				}
			}
			
			// re-writing the input
			for ( int i=0; i<allSen.get(s).size(); i++ ) {
					sb.append(allSen.get(s).get(i)+"\n");
			}
			
			sb.append("\n");
			sb.append(allSen.get(s+1).get(0)+"\n");			
			sb.append("\n");
			
			FileUtility.writeInFile(bioRelExInpFile, sb.toString(), true);
		}
	}
	
	
	/**
	 * 
	 * Note: This particular pre-processing didn't improve results in the LLL corpus.
	 * 
	 * @param bioRelExInpFile
	 * @param psgParsedFileName
	 * @throws Exception
	 */
	public String removeCommentsWithNoEntInParentheses( String sentence, ArrayList<int[]>  entBoundaryList) {
		
		ArrayList<String> words = DataStrucUtility.arrayToList(sentence.split("\\s+"));
		
		int bracS = -1, bracE = -1, tb = 0;
		boolean isFoundEnt = false;
		
		// the 1st item in the list is sentence id
		for ( int i=0; i<words.size(); i++ ) {
			if ( words.get(i).matches(".*\\)[?,.:;!]") ) {
				words.add(i+1, String.valueOf(words.get(i).charAt(words.get(i).length()-1)) );
				words.set( i, words.get(i).substring(0, words.get(i).length()-1) );
			}
			
			if ( words.get(i).charAt(0) == '(' ) {
				tb++;
				if ( tb == 1 ) {
					bracS = i;
					isFoundEnt = false;
				}
			}
			
			if ( words.get(i).charAt(words.get(i).length()-1) == ')' ) {
				tb--;
				if ( tb == 0 ) {
					bracE = i;
					
					int offset = 0, from = 0;
					for ( int w=0; w<=bracE; w++ ) {
						offset += words.get(w).length();
						if ( w < bracS )
							from = offset; 
					}
					
					for ( int e=0; e<entBoundaryList.size(); e++ ) {
						if ( entBoundaryList.get(e)[0] >= from && entBoundaryList.get(e)[1] <= offset ) {
							isFoundEnt = true;
							break;
						}
					}
					
					offset = offset - from;
					
					if ( !isFoundEnt ) {
						
						updateBoundaries(entBoundaryList, offset, from - 1, false);
							
						for ( int k=bracE; k>=bracS; k-- )
							words.remove(k);
						
						i=bracS-1;
					}
				}				
			}
		}
		
		sentence = "";
		
		for ( int i=0; i<words.size(); i++ )
			sentence += " " + words.get(i);
			
		return sentence.trim();
	}
		
	
	/**
	 * 
	 * @param entBoundaryList
	 * @param offset
	 * @param from
	 */
	private void updateBoundaries( ArrayList<int[]>  entBoundaryList, int offset, int from, boolean IsAdd ) {
		
		for ( int i=0; i<entBoundaryList.size(); i++ ) {
			if ( entBoundaryList.get(i)[0] > from )
				entBoundaryList.get(i)[0] = 
					IsAdd ? entBoundaryList.get(i)[0] + offset : entBoundaryList.get(i)[0] - offset;
			if ( entBoundaryList.get(i)[1] > from )
				entBoundaryList.get(i)[1] = 
					IsAdd ? entBoundaryList.get(i)[1] + offset : entBoundaryList.get(i)[1] - offset;
		}
	}
	
	
	/**
	 * 
	 * @param index
	 * @param entBoundaryList
	 * @param isStartIndex
	 * @return
	 */
	private boolean isAnEntityBoundaryIndex( int index, ArrayList<int[]> entBoundaryList, boolean isStartIndex) {
		int x = 0;
		if ( !isStartIndex )
			x = 1;
		
		for ( int i=0; i<entBoundaryList.size(); i++ )
			if ( entBoundaryList.get(i)[x] == index )
				return true;
		
		return false;
	}
	
	
	/**
	 * 
	 * @param sentence
	 * @param entBoundaryList
	 * @return
	 */
	public String removeDashAtWordBoundaryAndUpdateEntBoundaries( String sentence, ArrayList<int[]>  entBoundaryList) {
		
		String[] words = sentence.trim().split("\\s+");
	//	String origSen = sentence;
		sentence = "";
		int len = 0;
		
		for ( int wi=0; wi<words.length; wi++ ) {
			
			if ( words[wi].equals("-") && wi > 0 && isAnEntityBoundaryIndex(len-1, entBoundaryList, false)
									&& !isAnEntityBoundaryIndex(len+1, entBoundaryList, true) ) {
			
				if ( wi+1 < words.length && words[wi+1].matches("(and|or)") ) {
					boolean foundOtherSeparatedDashWord = false;
					for ( int v=wi+2; v<words.length && v<wi+7; v++ ) {
						if ( words[v].charAt(0) == '-' ) {
							foundOtherSeparatedDashWord = true;
							break;
						}	
					}
					
					if ( !foundOtherSeparatedDashWord ) {
						int z=0;
						for ( int v=wi+2; v<words.length && v<wi+7; v++ ) {
							if ( (z=words[v].indexOf("-")) > 0 ) {
								words[v] = words[v].replaceFirst("-", " -");
								break;
							}	
						}
						
						if ( z > 0 ) {
							String str = sentence + " " + words[wi] + " " + words[wi+1];
							for ( int v=wi+2; v<words.length; v++ ) {
								str = str + " " + words[v];
							}
							words = str.trim().split("\\s+");
						}
					}
				}
			}
				
			
			if ( words[wi].length() == 1 && 
					!( wi == words.length-1 || words[wi+1].matches("(and|or)")
							|| (words[wi].equals("-") && wi > 0 && isAnEntityBoundaryIndex(len-1, entBoundaryList, false)
									&& !isAnEntityBoundaryIndex(len+1, entBoundaryList, true) )
				) )
				; // do nothing
			else if ( words[wi].charAt(0) == '-' && !words[wi].matches("-[0-9].*") ) {
				if ( words[wi].length() == 1 )
					words[wi] = "";
				else
					words[wi] = words[wi].substring(1);
				updateBoundaries(entBoundaryList, 1, len, false);
			}
			else if ( words[wi].charAt(words[wi].length()-1) == '-' ) {
				words[wi] = words[wi].substring(0, words[wi].length()-1);
				updateBoundaries(entBoundaryList, 1, len+words[wi].length()-1, false);				
			}
			
            len += words[wi].length();
			sentence += " " + words[wi];
		}
		
		return sentence.replaceAll("\\s+", " ").trim();
	}
	
	
	public String preProcessWordWithDashEndingByAddingExtraWordAndUpdateEntBoundaries( String sentence, ArrayList<int[]>  entBoundaryList) {
		
		ArrayList<String> words = DataStrucUtility.arrayToList(sentence.split("\\s+"));
		sentence = "";
		int len = 0;
		
		for ( int wi=0; wi<words.size(); wi++ ) {
			if ( words.get(wi).length() == 1 )
				; // do nothing
			else if ( words.get(wi).matches(".*[0-9a-zA-Z]-") ) {
				
				int lastDashInd = -1;
				// identify the next word having dash
				// add the end token after the dash with the current word
				// we assume, the end token would not contain any entity name
				// we further assume, the end token is correct part that is meant to be related with the current word 
				for ( int x=wi+1; x <words.size(); x++ ) {
					if ( (lastDashInd = words.get(x).lastIndexOf("-")) > 0 && lastDashInd != words.get(x).length()-1 ) {
						//System.out.println(old);
						//System.out.println(words.get(wi));
						String newPart = words.get(x).substring(lastDashInd+1);
						words.set(wi, words.get(wi) + newPart);
						updateBoundaries(entBoundaryList, newPart.length(), len-1, true);
						break;
					}
				}
			}
			
			len += words.get(wi).length();
			
			sentence += " " + words.get(wi);
		}
		
		return sentence.trim();
	}
	
	
	public String preProcessWordWithDashEndingAndUpdateEntBoundaries( String sentence, ArrayList<int[]>  entBoundaryList) {
				  
		ArrayList<String> words = DataStrucUtility.arrayToList(sentence.split("\\s+"));
		String old = sentence;
		sentence = "";
		int len = 0;
		
		if ( old.contains(" of p42 MAPK were completely blocked by") )
			old.trim();
		
		for ( int wi=0; wi<words.size(); wi++ ) {
			if ( words.get(wi).length() == 1 )
				; // do nothing
			else if ( words.get(wi).matches(".*[a-zA-Z].*-") ) {
				
				/*
				 *  TODO: have to adjust entity boundaries for following entities and sentence
				 *  [liver-, islet-type glucokinase, glucokinase, actin]
				 *  When liver or islet type glucokinase was transiently expressed in COS-7 cells, the expressed glucokinase was also co-localized with actin filaments in the cytoplasm of these transfected cells.
				 */
				
				
				int lastDashInd = -1;
				// remove dash from the ending
				// identify the next word having dash
				// replace the dash of that word with space 
				for ( int x=wi+1; x <words.size(); x++ ) {
					if ( (lastDashInd = words.get(x).lastIndexOf("-")) > 0 && lastDashInd != words.get(x).length()-1 ) {
						//System.out.println(old);
						//System.out.println(words.get(wi));
						String newPart = words.get(x).substring(lastDashInd+1);
						words.set(wi, words.get(wi).substring(0, words.get(wi).length()-1));
						updateBoundaries(entBoundaryList, 1, len-1, false);
						
						words.set(x, words.get(x).substring(0, lastDashInd));
						words.add(x+1, newPart);
						updateBoundaries(entBoundaryList, 1, len-1, false);
						break;
					}
				}
			}
			
			len += words.get(wi).length();
			
			sentence += " " + words.get(wi);
		}
		
		return sentence.trim();
	}

	public String removeDashAtEndOfEntNamesAndUpdateEntBoundaries( String sentence, ArrayList<int[]>  entBoundaryList) {
		
		String[] words = sentence.split("\\s+");
		sentence = "";
		int len = 0;
		
		for ( int wi=0; wi<words.length; wi++ ) {
			if ( words[wi].charAt(0) == '-' ) {
				for ( int eb=0; eb<entBoundaryList.size(); eb++ ) {
					if ( entBoundaryList.get(eb)[1] == len-1 ) {
						words[wi] = words[wi].substring(1);
						updateBoundaries(entBoundaryList, 1, len-1, false);
						break;
					}
				}		
			}
			else if ( words[wi].charAt(words[wi].length()-1) == '-' ) {
				for ( int eb=0; eb<entBoundaryList.size(); eb++ ) {
					if ( entBoundaryList.get(eb)[1] == len + words[wi].length() ) {
						words[wi] = words[wi].substring(0, words[wi].length()-1);											
						updateBoundaries(entBoundaryList, 1, len+words[wi].length()-1, false);
						break;
					}
				}	
			}

			len += words[wi].length();			
			sentence += " " + words[wi];
		}
		
		return sentence.trim();
	}

	static int ca=0;
	
	/**
	 * Add comma before "and", "or" to avoid parsing errors
	 * NOTE: It decreased result on LLL.
	 * 
	 * @param sentence
	 * @param entBoundaryList
	 * @return
	 */
	public String addCommaBeforeConj( String sentence, ArrayList<int[]>  entBoundaryList) {
		
		String[] words = sentence.split("\\s+");
		
		sentence = words[0];
		int len = words[0].length();
		
		for ( int wi=1; wi<words.length; wi++ ) {
			if ( words[wi].matches("(and|or)")
					&& !words[wi-1].matches(".*[,;]")
					//&& words[wi-1].matches("(.*[A-Z]|.*[a-zA-Z].*[0-9])") 
					) {
				words[wi] = ", " + words[wi];
				updateBoundaries(entBoundaryList, 1, len, true);
				ca++;
				System.out.println(ca + ": " + words[wi-1] + words[wi]);
			}
			
			len += words[wi].length();			
			sentence += " " + words[wi];
		}
		
		System.out.println(sentence + "\n");
		return sentence.trim();
	} 
}
