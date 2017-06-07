package com.pingunaut.gitdeepsearch

import org.eclipse.jgit.api.Git
import java.nio.file.Files
import org.eclipse.jgit.lib.ObjectId
import java.nio.charset.StandardCharsets
import java.nio.file.Path
import java.nio.file.FileVisitOption
import java.nio.file.Paths
import java.util.stream.Collectors
import org.springframework.boot.autoconfigure.SpringBootApplication
import java.lang.Exception
import java.util.concurrent.Executors
import org.eclipse.jgit.lib.ObjectReader
import org.eclipse.jgit.revwalk.RevCommit
import org.eclipse.jgit.lib.Repository
import org.eclipse.jgit.revwalk.RevWalk

@SpringBootApplication
class Start

fun main(args: Array<String>) {
	if (args.size != 2) {
		println("please provide a local repository path (1) and a search regex (2) as parameter")
		System.exit(1);
	}

	println("Searching for ${args[1]} in ${args[0]}")

	val localPath = Paths.get(args[0]);
//	val localPath = Files.createTempDirectory("gitdeep");
	//do clone into a tmp folder, so the original repo is not modified
//	val git = Git.cloneRepository().setURI(Paths.get(args[0]).toString()).setDirectory(localPath.toFile()).call();
	// instead of checking all refs, we will walk over all object files to ensure that we'll find stuff which is no longer linked anywhere
//	unpackGitFiles(localPath)


	for (id in getIds(localPath)) {
		checkId(id, args[1], localPath)
	}
}

/**
 * Build git object IDs based on folder and file names
 */
private fun getIds(repo: Path): List<String> {
	//walk objects folder. build IDs using (two-digit) folder names + file names
	return Files.walk(Paths.get(repo.toString(), ".git", "objects"))
			.filter{ val name = it.parent.fileName.toString(); !(name.equals("objects")||name.equals("pack")||name.equals("info")) }
			.map { it.parent.fileName.toString() + it.fileName.toString() }.collect(Collectors.toList())
}

/**
 * if git packed objects already, unpack them to make them searchable easily
 */
private fun unpackGitFiles(repo: Path) {
	// create tmp dir
	val tmpPackFiles = Files.createTempDirectory(repo, "TMP_PACK_FILES")
	//mv all .pack files into tmp dir
	Files.walk(Paths.get(repo.toString(), ".git/objects/pack")).filter { it.toString().endsWith(".pack") }.forEach {
		val tmp = Paths.get(tmpPackFiles.toString(), it.getFileName().toString());
		Files.move(it, tmp)
		//unpack each pack file
		//TODO: beautify, add windows support... this is really ugly. jgit doesn't allow low level command unpack-objects... running it from command line here
		ProcessBuilder().command("/bin/sh", "-c", String.format("git unpack-objects < %s", tmp.getFileName().toString())).directory(tmpPackFiles.toFile()).redirectErrorStream(true).start().waitFor();
	}
}

/**
 * check if the object with the given id contains the given search term
 */
private fun checkId(id: String, searchTerm: String, localPath: Path) {
	val p = ProcessBuilder().command("/bin/sh", "-c", String.format("git cat-file -p %s", id)).directory(localPath.toFile()).redirectErrorStream(true).start()
	p.waitFor()
	val content = p.getInputStream().bufferedReader().use { it.readText() }  // defaults to UTF-8			
// check if it contains the search term, if yes print it
	if (content.contains(searchTerm)) {
		println("${id}:\n${content}");
	}

//	val objectReader = repo.newObjectReader()
//	var hit = false;
//	try {
//		// load object by id
//		val objId = ObjectId.fromString(id);
//		val objectLoader = objectReader.open(objId)
//		val type = objectLoader.getType();
//		val content = String(objectLoader.getBytes(), StandardCharsets.UTF_8)
//
//		// check if it contains the search term, if yes print it
//		if (content.contains(searchTerm)) {
//			println("${id}:\n${content}");
//			hit = true
//		}
//
//		if (!hit) {
//			val commit = getCommit(repo, objId)
//			val m = commit.getFullMessage()
////			println(type)
//			// check commit message also
//			if (m.contains(searchTerm)) {
//				println("${id}:\n${content}");
//				hit = true
//			}
//
//		}
//	} catch(e: Exception) {
//		//Just ignore for now
////		e.printStackTrace();
//	}
	}


	private fun getCommit(repo: Repository, id: ObjectId): RevCommit {
		val revWalk = RevWalk(repo)
		val commit = revWalk.parseCommit(id)
		revWalk.close()
		return commit
	}

