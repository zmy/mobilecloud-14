/*
 * 
 * Copyright 2014 Jules White
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *     http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * 
 */
package org.magnum.mobilecloud.video;

import java.io.IOException;
import java.security.Principal;
import java.util.Collection;

import javax.servlet.http.HttpServletResponse;

import org.magnum.mobilecloud.video.client.VideoSvcApi;
import org.magnum.mobilecloud.video.repository.Video;
import org.magnum.mobilecloud.video.repository.VideoRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.rest.webmvc.ResourceNotFoundException;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import com.google.common.collect.Lists;


@Controller
public class VideoSvc {

	// The VideoRepository that we are going to store our videos
	// in. We don't explicitly construct a VideoRepository, but
	// instead mark this object as a dependency that needs to be
	// injected by Spring. Our Application class has a method
	// annotated with @Bean that determines what object will end
	// up being injected into this member variable.
	//
	// Also notice that we don't even need a setter for Spring to
	// do the injection.
	//
	@Autowired
	private VideoRepository videos;

	// Receives GET requests to /video and returns the current
	// list of videos in memory. Spring automatically converts
	// the list of videos to JSON because of the @ResponseBody
	// annotation.
	@RequestMapping(value=VideoSvcApi.VIDEO_SVC_PATH, method=RequestMethod.GET)
	public @ResponseBody Collection<Video> getVideoList() {
		return Lists.newArrayList(videos.findAll());
	}

	// Receives POST requests to /video and converts the HTTP
	// request body, which should contain json, into a Video
	// object before adding it to the list. The @RequestBody
	// annotation on the Video parameter is what tells Spring
	// to interpret the HTTP request body as JSON and convert
	// it into a Video object to pass into the method. The
	// @ResponseBody annotation tells Spring to conver the
	// return value from the method back into JSON and put
	// it into the body of the HTTP response to the client.
	//
	// The VIDEO_SVC_PATH is set to "/video" in the VideoSvcApi
	// interface. We use this constant to ensure that the 
	// client and service paths for the VideoSvc are always
	// in synch.
	//
	@RequestMapping(value=VideoSvcApi.VIDEO_SVC_PATH, method=RequestMethod.POST)
	public @ResponseBody Video addVideo(@RequestBody Video v) {
		return videos.save(v);
	}

	@RequestMapping(value=VideoSvcApi.VIDEO_SVC_PATH+"/{id}", method=RequestMethod.GET)
	public @ResponseBody Video getVideo(@PathVariable("id") long id) {
		Video v = videos.findOne(id);
		if (v==null) {
			throw new ResourceNotFoundException("Video "+id);
		} else {
			return v;
		}
	}
	
	@RequestMapping(value=VideoSvcApi.VIDEO_SVC_PATH+"/{id}/like", method=RequestMethod.POST)
	public void likeVideo(
			@PathVariable("id") long id,
			Principal p,
			HttpServletResponse response) throws IOException {
		Video v = videos.findOne(id);
		if (v==null) {
			throw new ResourceNotFoundException("Video "+id);
		} else {
			String username = p.getName();
			if (v.alreadyLiked(username)) {
				response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Video "+id+" already liked.");
			} else {
				v.addLikeUser(username);
				videos.save(v);
				response.setStatus(HttpServletResponse.SC_OK);
			}
		}
	}
	
	@RequestMapping(value=VideoSvcApi.VIDEO_SVC_PATH+"/{id}/unlike", method=RequestMethod.POST)
	public void unlikeVideo(
			@PathVariable("id") long id,
			Principal p,
			HttpServletResponse response) throws IOException {
		Video v = videos.findOne(id);
		if (v==null) {
			throw new ResourceNotFoundException("Video "+id);
		} else {
			String username = p.getName();
			if (v.alreadyLiked(username)) {
				v.removeLikeUser(username);
				videos.save(v);
				response.setStatus(HttpServletResponse.SC_OK);
			} else {
				response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Video "+id+" not yet liked.");
			}
		}
	}
	
	@RequestMapping(value=VideoSvcApi.VIDEO_SVC_PATH+"/{id}/likedby", method=RequestMethod.GET)
	public @ResponseBody Collection<String> videoLikedBy(
			@PathVariable("id") long id,
			Principal p) {
		Video v = videos.findOne(id);
		if (v==null) {
			throw new ResourceNotFoundException("Video "+id);
		} else {
			return v.likedBy();
		}
	}
	
	// Receives GET requests to /video/find and returns all Videos
	// that have a title (e.g., Video.name) matching the "title" request
	// parameter value that is passed by the client
	@RequestMapping(value=VideoSvcApi.VIDEO_TITLE_SEARCH_PATH, method=RequestMethod.GET)
	public @ResponseBody Collection<Video> findByTitle(
			// Tell Spring to use the "title" parameter in the HTTP request's query
			// string as the value for the title method parameter
			@RequestParam(VideoSvcApi.TITLE_PARAMETER) String title) {
		return videos.findByName(title);
	}

	@RequestMapping(value=VideoSvcApi.VIDEO_DURATION_SEARCH_PATH, method=RequestMethod.GET)
	public @ResponseBody Collection<Video> findByMaxDuration(
			@RequestParam(VideoSvcApi.DURATION_PARAMETER) long maxduration) {
		return videos.findByDurationLessThan(maxduration);
	}
}
